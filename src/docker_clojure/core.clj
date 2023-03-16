(ns docker-clojure.core
  (:require
    [clojure.java.shell :refer [sh with-sh-dir]]
    [clojure.math.combinatorics :as combo]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.core.async :refer [<!! chan to-chan! pipeline-blocking] :as async]
    [docker-clojure.config :as cfg]
    [docker-clojure.dockerfile :as df]
    [docker-clojure.manifest :as manifest]
    [docker-clojure.util :refer [get-or-default default-docker-tag
                                 full-docker-tag]]
    [docker-clojure.log :refer [log] :as logger]
    [clojure.edn :as edn]))

(defn contains-every-key-value?
  "Returns true if the map `haystack` contains every key-value pair in the map
  `needles`. `haystack` may contain additional keys that are not in `needles`.
  Returns false if any of the keys in `needles` are missing from `haystack` or
  have different values."
  [haystack needles]
  (every? (fn [[k v]]
            (= v (get haystack k)))
          needles))

(defn base-image-tag [base-image jdk-version distro]
  (str base-image ":"
       (case base-image
         "eclipse-temurin" (str jdk-version "-jdk-")
         "debian" ""
         "-")
       (name distro)))

(defn exclude?
  "Returns true if `variant` matches one of `exclusions` elements (meaning
  `(contains-every-key-value? variant exclusion)` returns true)."
  [exclusions variant]
  (some (partial contains-every-key-value? variant) exclusions))

(s/def ::variant
  (s/keys :req-un [::cfg/jdk-version ::cfg/base-image ::cfg/base-image-tag
                   ::cfg/distro ::cfg/build-tool ::cfg/build-tool-version
                   ::cfg/maintainer ::cfg/docker-tag]
          :opt-un [::cfg/build-tool-versions ::cfg/architectures]))

(defn assoc-if [m pred k v]
  (if (pred)
    (assoc m k v)
    m))

(defn variant-map [[base-image jdk-version distro
                    [build-tool build-tool-version]]]
  (let [variant-arch (get cfg/distro-architectures
                          (-> distro namespace keyword))
        base         {:jdk-version        jdk-version
                      :base-image         base-image
                      :base-image-tag     (base-image-tag base-image
                                                          jdk-version distro)
                      :distro             distro
                      :build-tool         build-tool
                      :build-tool-version build-tool-version
                      :maintainer         (str/join " & " cfg/maintainers)}]
    (-> base
        (assoc :docker-tag (default-docker-tag base))
        (assoc-if #(nil? (:build-tool-version base)) :build-tool-versions
                  cfg/build-tools)
        (assoc-if #(seq variant-arch) :architectures variant-arch))))

(defn pull-image [image]
  (sh "docker" "pull" image))

(defn generate-dockerfile! [installer-hashes variant]
  (let [build-dir (df/build-dir variant)
        filename  "Dockerfile"]
    (log "Generating" (str build-dir "/" filename))
    (df/write-file build-dir filename installer-hashes variant)
    (assoc variant
      :build-dir build-dir
      :dockerfile filename)))

(defn build-image
  [installer-hashes {:keys [docker-tag base-image architectures] :as variant}]
  (let [image-tag     (str "clojure:" docker-tag)
        _             (log "Pulling base image" base-image)
        _             (pull-image base-image)

        {:keys [dockerfile build-dir]}
        (generate-dockerfile! installer-hashes variant)

        host-arch     (let [jvm-arch (System/getProperty "os.arch")]
                        (if (= "aarch64" jvm-arch)
                          "arm64v8"
                          jvm-arch))
        platform-flag (if (contains? (or architectures
                                         cfg/default-architectures)
                                     host-arch)
                        nil
                        (str "--platform=linux/" (first architectures)))

        build-cmd     (remove nil? ["docker" "buildx" "build" "--no-cache"
                                    "-t" image-tag platform-flag "--load"
                                    "-f" dockerfile "."])]
    (apply log "Running" build-cmd)
    (let [{:keys [out err exit]}
          (with-sh-dir build-dir (apply sh build-cmd))]
      (if (zero? exit)
        (log "Succeeded building" (str "clojure:" docker-tag))
        (log "ERROR building" (str "clojure:" docker-tag ":") err out))))
  (log)
  [::done variant])

(def latest-variant
  "The latest variant is special because we include all 3 build tools via the
  [::all] value on the end."
  (list (-> cfg/base-images :default first)
        cfg/default-jdk-version
        (get-or-default cfg/default-distros cfg/default-jdk-version)
        [::all]))

(defn image-variant-combinations [base-images jdk-versions distros build-tools]
  (reduce
    (fn [variants jdk-version]
      (concat
        variants
        (let [jdk-base-images (get-or-default base-images jdk-version)]
          (loop [[bi & r] jdk-base-images
                 acc #{}]
            (let [vs   (combo/cartesian-product #{bi}
                                                #{jdk-version}
                                                (get-or-default distros bi)
                                                build-tools)
                  acc' (concat acc vs)]
              (if (seq r)
                (recur r acc')
                acc'))))))
    #{} jdk-versions))

(defn image-variants [base-images jdk-versions distros build-tools]
  (into #{}
        (comp
          (map variant-map)
          (remove #(= ::s/invalid (s/conform ::variant %))))
        (conj
          (image-variant-combinations base-images jdk-versions distros
                                      build-tools)
          latest-variant)))

(defn rand-delay
  "Runs argument f w/ any supplied args after a random delay of 100-1000 ms"
  [f & args]
  (let [rand-time (+ 100 (rand-int 900))]
    (Thread/sleep rand-time)
    (apply f args)))

(defn build-images [parallelization installer-hashes variants]
  (log "Building images" parallelization "at a time")
  (let [variants-ch (to-chan! variants)
        builds-ch   (chan parallelization)]
    ;; Kick off builds with a random delay so we don't have Docker race
    ;; conditions (e.g. build container name collisions)
    (async/thread (pipeline-blocking parallelization builds-ch
                                     (map (partial rand-delay build-image
                                                   installer-hashes))
                                     variants-ch))
    (while (<!! builds-ch))))

(defn generate-dockerfiles! [installer-hashes variants]
  (doseq [variant variants]
    (generate-dockerfile! installer-hashes variant)))

(defn valid-variants []
  (remove (partial exclude? cfg/exclusions)
          (image-variants cfg/base-images cfg/jdk-versions cfg/distros
                          cfg/build-tools)))

(defn generate-manifest! [variants]
  (let [git-head (->> ["git" "rev-parse" "HEAD"] (apply sh) :out)
        manifest (manifest/generate {:maintainers   cfg/maintainers
                                     :architectures cfg/default-architectures
                                     :git-repo      cfg/git-repo}
                                    git-head variants)]
    (log "Writing manifest of" (count variants) "variants to clojure.manifest...")
    (spit "clojure.manifest" manifest)))

(defn sort-variants
  [variants]
  (sort
    (fn [v1 v2]
      (cond
        (= "latest" (:docker-tag v1)) -1
        (= "latest" (:docker-tag v2)) 1
        :else (let [c (compare (:jdk-version v1) (:jdk-version v2))]
                (if (not= c 0)
                  c
                  (let [c (compare (full-docker-tag v1) (full-docker-tag v2))]
                    (if (not= c 0)
                      c
                      (throw
                        (ex-info "No two variants should have the same full Docker tag"
                                 {:v1 v1, :v2 v2}))))))))
    variants))

(defn generate-variants
  [args]
  (let [key-vals (->> args
                      (map #(if (str/starts-with? % ":")
                              (edn/read-string %)
                              %)) ; TODO: Maybe replace this with bb/cli
                      (partition 2))
        variant-filter #(or
                          (empty? key-vals)
                          (every? (fn [[k v]]
                                    (= (get % k) v))
                                  key-vals))]
    (filter variant-filter (valid-variants))))

(defn run
  "Entrypoint for exec-fn. TODO: Make -main use this."
  [{:keys [cmd args parallelization]}]
  (logger/start)
  (let [variants (generate-variants args)]
    (log "Generated" (count variants) "variants")
    (case cmd
      :clean (df/clean-all)
      :dockerfiles (generate-dockerfiles! cfg/installer-hashes variants)
      :manifest (->> variants sort-variants generate-manifest!)
      :build-images (build-images parallelization cfg/installer-hashes variants)))
  (logger/stop))

(defn -main
  [& cmd-args]
  (let [[cmd & args] cmd-args]
    (run {:cmd                (if cmd (keyword cmd) :build-images)
          :args               args
          :parallelization    1})))
