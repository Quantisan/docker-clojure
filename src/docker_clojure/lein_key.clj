(ns docker-clojure.lein-key
  "Refreshes the local copy of Leiningen's release signing key that lein builds
  import (and check against the keyserver) instead of relying on the keyserver
  being up."
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
  "The parts of gpg `--with-colons` output that describe the key itself, the
  same fields lein builds compare against the keyserver."
  [colons]
  (->> (str/split-lines colons)
       (map #(str/split % #":" -1))
       (filter #(#{"pub" "sub" "uid" "fpr"} (first %)))
       (map #(str/join " " (map (fn [i] (get % i "")) [0 1 4 5 6 7 9 11])))
       (str/join "\n")))

(defn- gpg [home & args]
  (apply sh "gpg" "--batch" "--homedir" (str home) args))

(defn- temp-home []
  (Files/createTempDirectory
   "lein-key" (into-array FileAttribute
                          [(PosixFilePermissions/asFileAttribute
                            (PosixFilePermissions/fromString "rwx------"))])))

(defn- delete-tree [dir]
  (doseq [f (reverse (file-seq (io/file (str dir))))]
    (io/delete-file f true)))

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
        :else err))))

(defn refresh!
  "Fetches Leiningen's signing key from the keyserver, checks it's the pinned
  key, and writes it (minus third-party signatures) to `key-file`. Returns true
  on success."
  []
  (let [home (temp-home)]
    (try
      (println "Fetching" lein/signing-key "from" keyserver "...")
      (if-let [err (fetch! home)]
        (do (println "ERROR fetching key:" (str/trim err))
            false)
        (let [colons  (:out (gpg home "--with-colons" "--list-keys"))
              primary (primary-fingerprints colons)]
          (if (not= [lein/signing-key] primary)
            (do (println "ERROR: expected only key" lein/signing-key
                         "from the keyserver but got" (pr-str primary))
                false)
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
                    true))))))
      (finally
        (sh "gpgconf" "--homedir" (str home) "--kill" "all")
        (delete-tree home)))))
