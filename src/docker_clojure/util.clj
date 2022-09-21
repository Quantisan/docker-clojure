(ns docker-clojure.util
  (:require [docker-clojure.config :as cfg]
            [docker-clojure.core :as-alias core]
            [clojure.string :as str]))

(defn get-or-default
  "Returns the value in map m for key k or else the value for key :default."
  [m k]
  (get m k (get m :default)))

(defn jdk-label
  [omit-default? jdk-version base-image]
  (if (and omit-default? (= cfg/default-jdk-version jdk-version)
           (= (first (get-or-default cfg/base-images jdk-version))
              base-image))
    nil
    (str
      (case base-image
        ("eclipse-temurin" "debian") "temurin"
        base-image)
      "-" jdk-version)))

(defn docker-tag
  "Returns the Docker tag for the given variant with truthy keys from first arg
  left out when possible."
  [{:keys [omit-all? omit-jdk? omit-build-tool? omit-build-tool-version?
           omit-distro?]}
   {:keys [base-image jdk-version distro build-tool
           build-tool-version] :as _variant}]
  (if (= ::core/all build-tool)
    "latest"
    (let [jdk                      (jdk-label (or omit-all? omit-jdk?)
                                              jdk-version base-image)
          dd                       (get-or-default cfg/default-distros jdk-version)
          distro-label             (if (and (or omit-all? omit-distro?) (= dd distro))
                                     nil
                                     (when distro (name distro)))
          tag-elements             (remove nil? [jdk distro-label])
          build-tool-label         (if (and (seq tag-elements) ; ensure tag is non-empty
                                            (or omit-all? omit-build-tool?)
                                            (= build-tool cfg/default-build-tool))
                                     nil
                                     build-tool)
          build-tool-version-label (if (or omit-all? omit-build-tool?
                                           omit-build-tool-version?)
                                     nil
                                     build-tool-version)]
      (str/join "-" (remove nil? [jdk build-tool-label
                                  build-tool-version-label
                                  distro-label])))))

(def full-docker-tag
  (partial docker-tag {}))

(def default-docker-tag
  (partial docker-tag {:omit-jdk? true, :omit-distro? true}))
