(ns docker-clojure.dockerfile
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [docker-clojure.dockerfile.lein :as lein]
   [docker-clojure.dockerfile.tools-deps :as tools-deps]
   [docker-clojure.dockerfile.shared :refer [copy-resource-file! render-template]]
   [docker-clojure.log :refer [log]]))

(defn build-dir [{:keys [base-image-tag jdk-version build-tool]}]
  (str/join "/" ["target"
                 (str (str/replace base-image-tag ":" "-")
                      (when-not (str/includes? base-image-tag (str jdk-version))
                        (str "-" jdk-version)))
                 (if (= :docker-clojure.core/all build-tool)
                   "latest"
                   build-tool)]))

(defn all-prereqs [dir variant]
  (tools-deps/prereqs dir variant))

(defn copy-java?
  "Debian variants copy the JDK in from an eclipse-temurin image; the temurin
  base images already have it."
  [{:keys [distro]}]
  (contains? #{:debian :debian-slim} (-> distro namespace keyword)))

(defn entrypoint?
  "JDK 16+ ships a `repl` so we install our wrapper entrypoint for it."
  [{:keys [jdk-version]}]
  (>= jdk-version 16))

(defn for-build-tool
  "Return `variant` with `:build-tool-version` set to the given tool's version.
  Single-build-tool variants already carry it; the combined `latest` image
  pulls each tool's version from `:build-tool-versions`."
  [variant build-tool]
  (cond-> variant
    (nil? (:build-tool-version variant))
    (assoc :build-tool-version (get-in variant [:build-tool-versions build-tool]))))

(defn body
  "The build-tool install section(s) that go between the base image setup and
  the entrypoint. The combined `latest` image stacks both behind headers."
  [installer-hashes {:keys [build-tool] :as variant}]
  (case build-tool
    "lein"       (lein/install installer-hashes variant)
    "tools-deps" (tools-deps/install installer-hashes variant)
    :docker-clojure.core/all
    (str "\n### INSTALL LEIN ###\n"
         (lein/install installer-hashes (for-build-tool variant "lein"))
         "\n\n### INSTALL TOOLS-DEPS ###\n"
         (tools-deps/install installer-hashes (for-build-tool variant "tools-deps")))))

(defn command
  [{:keys [build-tool] :as variant}]
  (case build-tool
    "lein"                   (lein/command variant)
    "tools-deps"             (tools-deps/command variant)
    :docker-clojure.core/all "CMD [\"-M\", \"--repl\"]"))

(defn contents [installer-hashes variant]
  (render-template
   "templates/Dockerfile.tmpl"
   {:from        (:base-image-tag variant)
    :copy-java   (copy-java? variant)
    :jdk-version (:jdk-version variant)
    :body        (body installer-hashes variant)
    :entrypoint  (entrypoint? variant)
    :cmd         (command variant)}))

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

(defn generate! [installer-hashes variant]
  (let [build-dir (build-dir variant)
        filename  "Dockerfile"]
    (log "Generating" (str build-dir "/" filename))
    (write-file build-dir filename installer-hashes variant)
    (assoc variant
           :build-dir build-dir
           :dockerfile filename)))

(defn clean-all []
  (sh "sh" "-c" "rm -rf target/*"))
