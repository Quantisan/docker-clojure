(ns docker-clojure.dockerfile
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [docker-clojure.dockerfile.combined :as combined]
   [docker-clojure.dockerfile.shared :refer [copy-resource-file! entrypoint]]))

(defn build-dir [{:keys [base-image-tag jdk-version build-tool]}]
  (io/file "target"
           (str (str/replace base-image-tag ":" "-")
                (when-not (str/includes? base-image-tag (str jdk-version))
                  (str "-" jdk-version)))
           (name build-tool)))

(defn copy-java-from-temurin-contents
  [{:keys [jdk-version] :as _variant}]
  ["ENV JAVA_HOME=/opt/java/openjdk"
   (str "COPY --from=eclipse-temurin:" jdk-version " $JAVA_HOME $JAVA_HOME")
   "ENV PATH=\"${JAVA_HOME}/bin:${PATH}\""
   ""])

(defn contents [{:keys [distro base-image-tag] :as variant}]
  (->> (concat [(format "FROM %s" base-image-tag)
                ""]
               (case (-> distro namespace keyword)
                 (:debian :debian-slim) (copy-java-from-temurin-contents variant)
                 [])
               (combined/contents variant))
       (str/join "\n")))

(defn do-prereqs [dir {:keys [build-tool] :as variant}]
  (let [entrypoint (case build-tool
                     :tools-deps "clj"
                     :lein       "lein")]
    (copy-resource-file! dir "entrypoint"
                         #(str/replace % "@@entrypoint@@" entrypoint)
                         #(.setExecutable % true false)))
  (copy-resource-file! dir "rlwrap.retry" identity
                       #(.setExecutable % true false)))

(defn write-file [^java.io.File dir file variant]
  (.mkdirs dir)
  (do-prereqs dir variant)
  (spit (io/file dir file)
        (str (contents variant) "\n")))

(defn clean-all []
  (sh "sh" "-c" "rm -rf target/*"))
