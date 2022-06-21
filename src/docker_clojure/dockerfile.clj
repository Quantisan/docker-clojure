(ns docker-clojure.dockerfile
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [docker-clojure.dockerfile.boot :as boot]
   [docker-clojure.dockerfile.lein :as lein]
   [docker-clojure.dockerfile.tools-deps :as tools-deps]
   [docker-clojure.dockerfile.shared :refer :all]))

(defn build-dir [{:keys [base-image-tag build-tool]}]
  (str/join "/" ["target"
                 (str/replace base-image-tag ":" "-")
                 (if (= :docker-clojure.core/all build-tool)
                   "latest"
                   build-tool)]))

(defn all-prereqs [dir variant]
  (tools-deps/prereqs dir variant))

(defn all-contents [installer-hashes variant]
  (concat
    ["### INSTALL BOOT ###"]
    (boot/install
      installer-hashes
      (assoc variant :build-tool-version
             (get-in variant [:build-tool-versions "boot"])))
    ["" "### INSTALL LEIN ###"]
    (lein/install
      installer-hashes
      (assoc variant :build-tool-version
             (get-in variant [:build-tool-versions "lein"])))
    ["" "### INSTALL TOOLS-DEPS ###"]
    (tools-deps/install
      installer-hashes
     (assoc variant :build-tool-version
            (get-in variant [:build-tool-versions "tools-deps"])))
    [""]
    (entrypoint variant)
    ["" "CMD [\"-M\", \"--repl\"]"]))

(defn contents [installer-hashes {:keys [build-tool] :as variant}]
  (str/join "\n"
            (concat [(format "FROM %s" (:base-image-tag variant))
                     ""]
                    (case build-tool
                      :docker-clojure.core/all (all-contents installer-hashes variant)
                      "boot" (boot/contents installer-hashes variant)
                      "lein" (lein/contents installer-hashes variant)
                      "tools-deps" (tools-deps/contents installer-hashes variant)))))

(defn shared-prereqs [dir {:keys [build-tool]}]
  (let [entrypoint (case build-tool
                     "tools-deps"             "clj"
                     :docker-clojure.core/all "clj"
                     build-tool)]
    (copy-resource-file! dir "entrypoint"
                         #(str/replace % "@@entrypoint@@" entrypoint)
                         #(.setExecutable % true false))))

(defn do-prereqs [dir {:keys [build-tool] :as variant}]
  (shared-prereqs dir variant)
  (case build-tool
    :docker-clojure.core/all (all-prereqs dir variant)
    "boot" (boot/prereqs dir variant)
    "lein" (lein/prereqs dir variant)
    "tools-deps" (tools-deps/prereqs dir variant)))

(defn write-file [dir file installer-hashes variant]
  (let [{:keys [exit err]} (sh "mkdir" "-p" dir)]
    (if (zero? exit)
      (do
        (do-prereqs dir variant)
        (spit (str/join "/" [dir file])
              (str (contents installer-hashes variant) "\n")))
      (throw (ex-info (str "Error creating directory " dir)
                      {:error err})))))

(defn clean-all []
  (sh "sh" "-c" "rm -rf target/*"))
