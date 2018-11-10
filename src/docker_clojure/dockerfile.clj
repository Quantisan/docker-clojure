(ns docker-clojure.dockerfile
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.string :as str]
   [docker-clojure.dockerfile.boot :as boot]
   [docker-clojure.dockerfile.lein :as lein]
   [docker-clojure.dockerfile.tools-deps :as tools-deps]))

(defn base-image-tag [{:keys [base-image distro]}]
  (if (= "debian" distro)
    base-image
    (str base-image "-" distro)))

(defn base-image-filename [variant]
  (-> variant
      base-image-tag
      (str/replace ":" "-")))

(defn filename [{:keys [build-tool] :as variant}]
  (str/join "." ["Dockerfile"
                 (base-image-filename variant)
                 build-tool]))

(defn contents [{:keys [maintainer build-tool] :as variant}]
  (str/join "\n"
            (concat [(format "FROM %s" (base-image-tag variant))
                     (format "LABEL maintainer=\"%s\"" maintainer)
                     ""]
                    (case build-tool
                      "boot" (boot/contents variant)
                      "lein" (lein/contents variant)
                      "tools-deps" (tools-deps/contents variant)))))

(defn write-file [file variant]
  (spit file (contents variant)))

(defn clean-all []
  (sh "sh" "-c" "rm Dockerfile.*"))
