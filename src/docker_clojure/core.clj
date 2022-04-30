(ns docker-clojure.core
  (:require
    [clojure.java.shell :refer [sh with-sh-dir]]
    [clojure.math.combinatorics :as combo]
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [docker-clojure.dockerfile :as df]))

(s/def ::non-blank-string
  (s/and string? #(not (str/blank? %))))

(s/def ::jdk-version
  (s/and pos-int? #(<= 8 %)))
(s/def ::jdk-versions (s/coll-of ::jdk-version :distinct true :into #{}))

(s/def ::base-image ::non-blank-string)
(s/def ::base-images (s/coll-of ::base-image :distinct true :into #{}))

(s/def ::distro qualified-keyword?)
(s/def ::distros (s/coll-of ::distro :distinct true :into #{}))

(s/def ::build-tool (s/or ::specific-tool ::non-blank-string
                          ::all-tools #(= ::all %)))
(s/def ::build-tool-version
  (s/nilable (s/and ::non-blank-string #(re-matches #"[\d\.]+" %))))
(s/def ::build-tools (s/map-of ::build-tool ::build-tool-version))

(s/def ::exclusions
  (s/keys :opt-un [::jdk-version ::distro ::build-tool ::build-tool-version]))

(s/def ::maintainers
  (s/coll-of ::non-blank-string :distinct true :into #{}))

(def jdk-versions #{8 11 17 18})

(def base-images
  "Map of JDK version to base image name with :default as a fallback"
  {8        "openjdk"
   11       "openjdk"
   :default "eclipse-temurin"})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 17)

(def distros
  "Map of base image name to set of distro tags to use, namespaced by Linux
  distro type. :default key is a fallback for base images not o/w specified."
  {"openjdk" #{:debian/buster :debian-slim/slim-buster :debian/bullseye
               :debian-slim/slim-bullseye :alpine/alpine}
   :default  #{:alpine/alpine :ubuntu/focal}})

(def default-distros
  "The default distro to use for tags that don't specify one, keyed by jdk-version.
  :default is a fallback for jdk versions not o/w specified."
  {8        :debian-slim/slim-bullseye
   11       :debian-slim/slim-bullseye
   :default :ubuntu/focal})

(def build-tools
  {"lein"       "2.9.8"
   "boot"       "2.8.3"
   "tools-deps" "1.11.1.1113"})

(def installer-hashes
  {"lein"       {"2.9.6" "094b58e2b13b42156aaf7d443ed5f6665aee27529d9512f8d7282baa3cc01429"
                 "2.9.7" "f78f20d1931f028270e77bc0f0c00a5a0efa4ecb7a5676304a34ae4f469e281d"
                 "2.9.8" "9952cba539cc6454c3b7385ebce57577087bf2b9001c3ab5c55d668d0aeff6e9"}
   "boot"       {"2.8.3" "0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3"}
   "tools-deps" {"1.11.0.1100" "a71bd520bd43d4be6e0cab0c525f5d1f85911fc276f3d0f37f00243fb0f1e594"
                 "1.11.1.1105" "5655c3ee3ea495d0778d8a87ce05a719045d3ceae9dd5cc29033379d8f82cce5"
                 "1.11.1.1113" "7677bb1179ebb15ebf954a87bd1078f1c547673d946dadafd23ece8cd61f5a9f"}})

(def exclusions ; don't build these for whatever reason(s)
  #{{:jdk-version 8
     :distro      :alpine/alpine}
    {:jdk-version 11
     :distro      :alpine/alpine}})

(def maintainers
  "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>")

(defn get-or-default
  "Returns the value in map m for key k or else the value for key :default."
  [m k]
  (get m k (get m :default)))

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
  (str base-image ":" jdk-version
       (case base-image
         "eclipse-temurin" "-jdk-"
         "-")
       (name distro)))

(defn exclude?
  "Returns true if `variant` matches one of `exclusions` elements (meaning
  `(contains-every-key-value? variant exclusion)` returns true)."
  [exclusions variant]
  (some (partial contains-every-key-value? variant) exclusions))

(defn jdk-label [jdk-version base-image]
  (if (= default-jdk-version jdk-version)
    nil
    (str
      (case base-image
        "eclipse-temurin" "temurin"
        base-image)
      "-" jdk-version)))

(defn docker-tag
  [{:keys [base-image jdk-version distro build-tool
           build-tool-version]}]
  (if (= ::all build-tool)
    "latest"
    (let [jdk          (jdk-label jdk-version base-image)
          dd           (get-or-default default-distros jdk-version)
          distro-label (if (= dd distro) nil (when distro (name distro)))]
      (str/join "-" (remove nil? [jdk build-tool build-tool-version
                                  distro-label])))))

(s/def ::variant
  (s/keys :req-un [::jdk-version ::base-image ::distro ::build-tool
                   ::build-tool-version ::maintainer ::docker-tag]
          :opt-un [::build-tool-versions]))

(defn assoc-if [m pred k v]
  (if (pred)
    (assoc m k v)
    m))

(defn variant-map [[base-image jdk-version distro [build-tool build-tool-version]]]
  (let [base {:jdk-version        jdk-version
              :base-image         base-image
              :base-image-tag     (base-image-tag base-image jdk-version distro)
              :distro             distro
              :build-tool         build-tool
              :build-tool-version build-tool-version
              :maintainer         maintainers}]
    (-> base
        (assoc :docker-tag (docker-tag base))
        (assoc-if #(nil? (:build-tool-version base)) :build-tool-versions build-tools))))

(defn pull-image [image]
  (sh "docker" "pull" image))

(defn generate-dockerfile! [installer-hashes variant]
  (let [build-dir (df/build-dir variant)
        filename  "Dockerfile"]
    (println "Generating" (str build-dir "/" filename))
    (df/write-file build-dir filename installer-hashes variant)
    (assoc variant
      :build-dir build-dir
      :dockerfile filename)))

(defn build-image [installer-hashes {:keys [docker-tag base-image] :as variant}]
  (let [image-tag (str "clojure:" docker-tag)
        _         (println "Pulling base image" base-image)
        _         (pull-image base-image)

        {:keys [dockerfile build-dir]}
        (generate-dockerfile! installer-hashes variant)

        build-cmd (remove nil? ["docker" "build" "--no-cache" "-t" image-tag
                                "--load" "-f" dockerfile "."])]
    (apply println "Running" build-cmd)
    (let [{:keys [out err exit]}
          (with-sh-dir build-dir (apply sh build-cmd))]
      (if (zero? exit)
        (println "Succeeded")
        (do
          (println "ERROR:" err)
          (print out)))))
  (println))

(def latest-variant
  "The latest variant is special because we include all 3 build tools via the
  [::all] value on the end."
  (list (:default base-images)
        default-jdk-version
        (get-or-default default-distros default-jdk-version)
        [::all]))

(defn image-variant-combinations [base-images jdk-versions distros build-tools]
  (reduce
    (fn [variants jdk-version]
      (concat
        variants
        (let [base-image (get-or-default base-images jdk-version)]
          (combo/cartesian-product #{(get-or-default base-images jdk-version)}
                                   #{jdk-version}
                                   (get-or-default distros base-image)
                                   build-tools))))
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

(defn build-images [installer-hashes variants]
  (println "Building images")
  (doseq [variant variants]
    (build-image installer-hashes variant)))

(defn generate-dockerfiles! [installer-hashes variants]
  (doseq [variant variants]
    (generate-dockerfile! installer-hashes variant)))

(defn valid-variants []
  (remove (partial exclude? exclusions)
          (image-variants base-images jdk-versions distros build-tools)))

(defn -main [& args]
  (case (first args)
    "clean" (df/clean-all)
    "dockerfiles" (generate-dockerfiles! installer-hashes (valid-variants))
    (build-images installer-hashes (valid-variants)))
  (System/exit 0))
