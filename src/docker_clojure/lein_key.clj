(ns docker-clojure.lein-key
  "Manages the local copy of Leiningen's release signing key that lein builds
  import instead of fetching it from a keyserver. CI checks the copy against
  the keyserver so changes to the key (e.g. a revocation) get noticed."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [docker-clojure.dockerfile.lein :as lein])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute PosixFilePermissions)))

(def keyserver "hkps://keyserver.ubuntu.com")

(def key-file (io/file "resources" lein/signing-key-file))

(def ^:private fetch-attempts 3)

(defn primary-fingerprints
  "Returns the fingerprints of the primary keys in gpg `--with-colons` output.
  Each `pub` record is followed by its `fpr` record; subkeys' `fpr` records
  follow `sub` records instead."
  [colons]
  (->> (str/split-lines colons)
       (map #(str/split % #":" -1))
       (partition 2 1)
       (keep (fn [[prev cur]]
               (when (and (= "pub" (first prev)) (= "fpr" (first cur)))
                 (nth cur 9))))))

(defn summary
  "The parts of gpg `--with-colons` output that describe the key itself:
  validity (incl. revocation), key IDs, creation & expiry dates, user IDs,
  fingerprints and capabilities. Leaves out where and when gpg imported it,
  which differs between copies of the same key."
  [colons]
  (->> (str/split-lines colons)
       (map #(str/split % #":" -1))
       (filter #(#{"pub" "sub" "uid" "fpr"} (first %)))
       (map #(str/join " " (map (fn [i] (get % i "")) [0 1 4 5 6 7 9 11])))
       (str/join "\n")))

(defn- gpg [home & args]
  (apply sh "gpg" "--batch" "--homedir" (str home) args))

(defn- delete-tree [dir]
  (doseq [f (reverse (file-seq (io/file (str dir))))]
    (io/delete-file f true)))

(defn- with-temp-home
  "Calls `f` with a new, empty gpg home dir and cleans it up afterwards."
  [f]
  (let [home (Files/createTempDirectory
              "lein-key"
              (into-array FileAttribute
                          [(PosixFilePermissions/asFileAttribute
                            (PosixFilePermissions/fromString "rwx------"))]))]
    (try
      (f home)
      (finally
        (sh "gpgconf" "--homedir" (str home) "--kill" "all")
        (delete-tree home)))))

(defn- list-key [home]
  (:out (gpg home "--with-colons" "--list-keys")))

(defn- fetch!
  "Fetches the signing key into `home`, retrying since the keyserver is flaky.
  Returns nil on success or the last error."
  [home]
  (loop [attempt 1]
    (let [{:keys [exit err]} (gpg home "--keyserver" keyserver
                                  "--recv-keys" lein/signing-key)]
      (cond
        (zero? exit) nil
        (< attempt fetch-attempts) (do (println "Fetch attempt" attempt
                                                "failed, retrying:" (str/trim err))
                                       (Thread/sleep (* 2000 attempt))
                                       (recur (inc attempt)))
        :else (str/trim err)))))

(defn- pinned-key?
  "True if `colons` lists exactly the pinned signing key; prints why not
  otherwise."
  [what colons]
  (let [primary (primary-fingerprints colons)]
    (or (= [lein/signing-key] primary)
        (do (println "ERROR: expected only key" lein/signing-key "in" what
                     "but got" (pr-str primary))
            false))))

(defn- github-annotation
  "Prints a GitHub Actions annotation so it shows on the run summary."
  [level msg]
  (when (System/getenv "GITHUB_ACTIONS")
    (println (str "::" level "::" msg))))

(defn check!
  "Checks the local copy of the signing key against the keyserver. Returns true
  if they match or the keyserver can't be reached (warning about it), false if
  they differ."
  []
  (println "Checking" (str key-file) "against" keyserver "...")
  (with-temp-home
    (fn [local-home]
      (with-temp-home
        (fn [remote-home]
          (let [{:keys [exit err]} (gpg local-home "--import" (str key-file))
                local-colons (list-key local-home)]
            (cond
              (not (zero? exit))
              (do (println "ERROR importing" (str key-file) err) false)

              (not (pinned-key? key-file local-colons))
              false

              :else
              (if-let [err (fetch! remote-home)]
                (let [msg (str "Couldn't fetch " lein/signing-key " from "
                               keyserver " to check " key-file ": " err)]
                  (println "WARNING:" msg)
                  (github-annotation "warning" msg)
                  true)
                (let [remote-colons (list-key remote-home)
                      local         (summary local-colons)
                      remote        (summary remote-colons)]
                  (cond
                    (not (pinned-key? keyserver remote-colons))
                    false

                    (= local remote)
                    (do (println (str key-file) "matches the keyserver.")
                        true)

                    :else
                    (let [msg (str "Leiningen's signing key " lein/signing-key
                                   " on " keyserver " no longer matches "
                                   key-file ". Review the change, then run"
                                   " `bb refresh-lein-key`.")]
                      (println "ERROR:" msg)
                      (println "It may have been revoked, had its expiry"
                               "changed, or gained a subkey or user ID.")
                      (println (str "\nLocal copy:\n" local
                                    "\n\nKeyserver copy:\n" remote "\n"))
                      (github-annotation "error" msg)
                      false)))))))))))

(defn refresh!
  "Fetches Leiningen's signing key from the keyserver, checks it's the pinned
  key, and writes it (minus third-party signatures) to `key-file`. Returns true
  on success."
  []
  (with-temp-home
    (fn [home]
      (println "Fetching" lein/signing-key "from" keyserver "...")
      (if-let [err (fetch! home)]
        (do (println "ERROR fetching key:" err)
            false)
        (let [colons (list-key home)]
          (when (pinned-key? keyserver colons)
            (let [{:keys [exit out err]}
                  (gpg home "--armor" "--export-options" "export-minimal"
                       "--export" lein/signing-key)
                  old (when (.exists key-file) (slurp key-file))]
              (if-not (and (zero? exit) (str/includes? out "BEGIN PGP"))
                (do (println "ERROR exporting key:" (str/trim err))
                    false)
                (do (spit key-file out)
                    (println (if (= old out)
                               (str key-file " is already up to date.")
                               (str "Updated " key-file ".")))
                    (println (str "\n" (summary colons) "\n"))
                    (when (not= old out)
                      (println "Review the change (e.g. a revocation or new"
                               "subkey) before committing it, then run"
                               "`bb dockerfiles`."))
                    true)))))))))
