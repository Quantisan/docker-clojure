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

(def base-images #{"openjdk" "ghcr.io/graalvm/graalvm-ce"})

(def default-base-image "openjdk")

(def jdk-versions #{8 11 16 17 18})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 11)

(def distros
  {"openjdk"                    #{:debian/buster :debian-slim/slim-buster :debian/bullseye
                                  :debian-slim/slim-bullseye :alpine/alpine}
   "ghcr.io/graalvm/graalvm-ce" #{:oracle-linux/ol8}})

;; The default distro to use for tags that don't specify one, keyed by base-image.
(def default-distros
  {"openjdk"                    :debian-slim/slim-bullseye
   "ghcr.io/graalvm/graalvm-ce" :oracle-linux/ol8})

(def build-tools
  {"lein"       "2.9.6"
   "boot"       "2.8.3"
   "tools-deps" "1.10.3.967"})

(def exclusions                                             ; don't build these for whatever reason(s)
  #{{:jdk-version 8
     :distro      :alpine/alpine}
    {:jdk-version 11
     :distro      :alpine/alpine}
    {:jdk-version 16
     :distro      :alpine/alpine}
    {:jdk-version 17
     :distro      :alpine/alpine}
    {:base-image "ghcr.io/graalvm/graalvm-ce:java16"}
    {:base-image "ghcr.io/graalvm/graalvm-ce:java17"}
    {:base-image "ghcr.io/graalvm/graalvm-ce:java18"}})

(def maintainers
  "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>")

(defn default-distro [base-image]
  (get default-distros base-image (:default default-distros)))

(defn contains-every-key-value?
  "Returns true if the map `haystack` contains every key-value pair in the map
  `needles`. `haystack` may contain additional keys that are not in `needles`.
  Returns false if any of the keys in `needles` are missing from `haystack` or
  have different values."
  [haystack needles]
  (every? (fn [[k v]]
            (= v (get haystack k)))
          needles))

(defn base-image-short-name [base-image]
  (cond-> base-image
    (str/includes? base-image "/") (-> (str/split #"/") second)
    (str/includes? base-image ":") (-> (str/split #":") first)))

(defn jdk-version-tag [base-image jdk-version]
  (case base-image
    "openjdk" jdk-version
    "ghcr.io/graalvm/graalvm-ce" (str "java" jdk-version)))

(defn base-image-tag [base-image jdk-version distro]
  (let [base (str base-image ":" (jdk-version-tag base-image jdk-version))]
    (case base-image
      "openjdk" (str base "-" (name distro))
      "ghcr.io/graalvm/graalvm-ce" base)))

(defn exclude?
  "Returns true if `variant` matches one of `exclusions` elements (meaning
  `(contains-every-key-value? variant exclusion)` returns true)."
  [exclusions variant]
  (some (partial contains-every-key-value? variant) exclusions))

(defn jdk-label [base-image jdk-version]
  (let [bisn (base-image-short-name base-image)]
    (if (and (= bisn default-base-image)
             (= default-jdk-version jdk-version))
      nil
      (str bisn "-" jdk-version))))

(defn docker-tag
  [{:keys [base-image jdk-version distro build-tool build-tool-version]
    :or   {base-image default-base-image}}]
  (if (= ::all build-tool)
    "latest"
    (let [jdk-tag (jdk-label base-image jdk-version)
          dd (default-distro base-image)
          distro-label (if (= dd distro) nil (when distro (name distro)))]
      (str/join "-" (remove nil? [jdk-tag build-tool build-tool-version
                                  distro-label])))))

(s/def ::variant
  (s/keys :req-un [::jdk-version ::base-image ::distro ::build-tool
                   ::build-tool-version ::maintainer ::docker-tag]
          :opt-un [::build-tool-versions]))

(defn assoc-if [m pred k v]
  (if (pred)
    (assoc m k v)
    m))

(defn variant-map [[base-image jdk-version distro [build-tool build-tool-version] :as all]]
  (let [base {:jdk-version        jdk-version
              :base-image         (base-image-tag base-image jdk-version distro)
              :distro             distro
              :build-tool         build-tool
              :build-tool-version build-tool-version
              :maintainer         maintainers}]
    (-> base
        (assoc :docker-tag (docker-tag base))
        (assoc-if #(nil? (:build-tool-version base)) :build-tool-versions build-tools))))

(defn pull-image [image]
  (sh "docker" "pull" image))

(defn build-image [{:keys [docker-tag dockerfile build-dir base-image] :as variant}]
  (let [image-tag (str "clojure:" docker-tag)
        ;; TODO: Build for all appropriate platforms instead of just linux/amd64.
        ;;       alpine & JDK 8 won't build for arm64.
        build-cmd ["docker" "buildx" "build" "--no-cache" "--platform"
                   "linux/amd64" "--load" "-t" image-tag "-f" dockerfile "."]]
    (println "Pulling base image" base-image)
    (pull-image base-image)
    (df/write-file build-dir dockerfile variant)
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
  (list default-base-image default-jdk-version
        (default-distro default-base-image) [::all]))

(defn image-variants [base-image jdk-versions distros build-tools]
  (cond->> (combo/cartesian-product jdk-versions (get distros base-image)
                                     build-tools)
           true (map #(cons base-image %))
           (= base-image "openjdk") (cons latest-variant)
           true (map variant-map)
           true (remove #(= ::s/invalid (s/conform ::variant %)))
           true set))

(defn build-images [variants]
  (println "Building images")
  (doseq [variant variants]
    (when-not (exclude? exclusions variant)
      (build-image variant))))

(defn generate-dockerfile! [variant]
  (let [build-dir (df/build-dir variant)
        filename "Dockerfile"]
    (println "Generating" (str build-dir "/" filename))
    (df/write-file build-dir filename variant)
    (assoc variant
      :build-dir build-dir
      :dockerfile filename)))

(defn generate-dockerfiles! [base-image]
  (for [variant (image-variants base-image jdk-versions distros build-tools)
        :when (not (exclude? exclusions variant))]
    (generate-dockerfile! variant)))

(defn -main [& args]
  (case (first args)
    "clean" (df/clean-all)
    "dockerfiles" (dorun (map #(dorun (generate-dockerfiles! %)) base-images))
    (build-images (map generate-dockerfiles! base-images)))
  (System/exit 0))
