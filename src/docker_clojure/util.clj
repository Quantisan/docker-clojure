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
    (str "temurin-" jdk-version)))

(defn docker-tag
  "Returns the Docker tag for the given variant with truthy keys from first arg
  left out when possible."
  [{:keys [omit-jdk? omit-build-tool? omit-build-tool-version? omit-distro?]}
   {:keys [base-image jdk-version distro build-tool
           build-tool-version] :as _variant}]
  (let [default-distro (get-or-default cfg/default-distros jdk-version)
        distro-label (if (and omit-distro? (= default-distro distro))
                       nil
                       (name distro))
        build-tool-label (if (and omit-build-tool?
                                  (= build-tool cfg/default-build-tool))
                           nil
                           (name build-tool))
        build-tool-version-label (if (or omit-build-tool? omit-build-tool-version?)
                                   nil
                                   build-tool-version)
        jdk (jdk-label (and omit-jdk?
                            ;; Can't let distro be the only tag.
                            (or (nil? distro-label) build-tool-label))
                       jdk-version base-image)
        tags (remove nil? [jdk build-tool-label build-tool-version-label
                           distro-label])]
    (if (seq tags)
      (str/join "-" tags)
      ;; No tags means we are tagging "latest".
      "latest")))

(def full-docker-tag
  (partial docker-tag {}))

(def default-docker-tag
  (partial docker-tag {:omit-jdk? true, :omit-distro? true}))
