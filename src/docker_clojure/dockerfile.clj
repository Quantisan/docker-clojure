(ns docker-clojure.dockerfile
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [docker-clojure.dockerfile.boot :as boot]
   [docker-clojure.dockerfile.lein :as lein]
   [docker-clojure.dockerfile.tools-deps :as tools-deps]))

(defn build-dir [{:keys [base-image build-tool]}]
  (str/join "/" ["target"
                 (str/replace base-image ":" "-")
                 build-tool]))

(defn contents [{:keys [maintainer build-tool] :as variant}]
  (str/join "\n"
            (concat [(format "FROM %s" (:base-image variant))
                     (format "LABEL maintainer=\"%s\"" maintainer)
                     ""]
                    (case build-tool
                      "boot" (boot/contents variant)
                      "lein" (lein/contents variant)
                      "tools-deps" (tools-deps/contents variant)))))

(defn write-file [dir file variant]
  (let [{:keys [exit err]} (sh "mkdir" "-p" dir)]
    (if (zero? exit)
      (spit (str/join "/" [dir file])
            (contents variant))
      (throw (ex-info (str "Error creating directory " dir)
                      {:error err})))))

(defn clean-all []
  (sh "sh" "-c" "rm -rf target/*"))
