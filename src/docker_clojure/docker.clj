(ns docker-clojure.docker
  (:require [clojure.java.shell :refer [sh with-sh-dir]]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [docker-clojure.config :as cfg]
            [docker-clojure.core :as-alias core]
            [docker-clojure.dockerfile :as df]
            [docker-clojure.log :refer [log]]
            [docker-clojure.util :refer [get-or-default]]
            [docker-clojure.variant :as-alias variant]))

(defn pull-image [image]
  (sh "docker" "pull" image))

(defn smoke-test-cmds
  "Commands to run in a freshly built image to check its build tool(s) work."
  [{:keys [build-tool]}]
  (let [cmds {"lein"       ["lein" "version"]
              "tools-deps" ["clojure" "-M" "-e" "(clojure-version)"]}]
    (if (= ::core/all build-tool)
      (vals cmds)
      [(get cmds build-tool)])))

(defn smoke-test
  "Runs the variant's smoke test commands in `image-tag`. Returns true if they
  all succeed."
  [image-tag platform-flag variant]
  (every? (fn [cmd]
            (let [run-cmd (remove nil? (concat ["docker" "run" "--rm"
                                                platform-flag image-tag]
                                               cmd))
                  _       (apply log "Running" run-cmd)
                  {:keys [out err exit]} (apply sh run-cmd)]
              (if (zero? exit)
                true
                (do (log "ERROR smoke testing" (str image-tag ":") err out)
                    false))))
          (smoke-test-cmds variant)))

(defn build-image
  "Builds and smoke tests the image for `variant`. Returns [::done variant] on
  success or [::failed variant] otherwise."
  [installer-hashes {:keys [docker-tag base-image architecture] :as variant}]
  (let [image-tag     (str "clojure:" docker-tag)
        _             (log "Pulling base image" base-image)
        _             (pull-image base-image)

        {:keys [dockerfile build-dir]}
        (df/generate! installer-hashes variant)

        host-arch     (let [jvm-arch (System/getProperty "os.arch")]
                        (if (= "aarch64" jvm-arch)
                          "arm64v8"
                          jvm-arch))
        platform-flag (if (= architecture host-arch)
                        nil
                        (str "--platform=linux/" architecture))

        build-cmd     (remove nil? ["docker" "buildx" "build" "--no-cache"
                                    "-t" image-tag platform-flag "--load"
                                    "-f" dockerfile "."])
        _             (apply log "Running" build-cmd)
        {:keys [out err exit]}
        (with-sh-dir build-dir (apply sh build-cmd))
        built?        (zero? exit)
        _             (if built?
                        (log "Succeeded building" image-tag)
                        (log "ERROR building" (str image-tag ":") err out))
        ok?           (and built? (smoke-test image-tag platform-flag variant))]
    (log)
    [(if ok? ::done ::failed) variant]))

(defn base-image-tag
  [base-image jdk-version distro]
  (str base-image ":"
       (case base-image
         "eclipse-temurin" (str jdk-version "-jdk-")
         "debian" ""
         "-")
       (name distro)))

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

(defn tag
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

(def full-tag
  (partial tag {}))

(def default-tag
  (partial tag {:omit-jdk? true, :omit-distro? true}))

(defn all-tags
  "Returns all Docker tags for the given variant"
  [variant]
  (let [short-tag (:docker-tag variant)
        full-tag  (full-tag variant)
        base      (into #{} [short-tag full-tag])]
    (-> base
        (conj
         (tag {:omit-jdk? true} variant)
         (tag {:omit-build-tool? true} variant)
         (tag {:omit-build-tool-version? true} variant)
         (tag {:omit-distro? true} variant)
         (tag {:omit-distro? true, :omit-build-tool-version? true} variant)
         (tag {:omit-jdk? true, :omit-build-tool-version? true} variant)
         (tag {:omit-jdk?                true, :omit-distro? true
               :omit-build-tool-version? true} variant))
        vec
        sort)))

(s/fdef all-tags
  :args (s/cat :variant ::variant/variant)
  :ret  (s/coll-of ::cfg/docker-tag))
