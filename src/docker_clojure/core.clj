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

(s/def ::distro ::non-blank-string)
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

(def base-image "openjdk")

(def jdk-versions #{8 11 15 16})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 11)

(def distros
  #{"buster" "slim-buster" "alpine"})

;; The default distro to use for tags that don't specify one, keyed by jdk-version.
(def default-distros
  {:default "slim-buster"})

(def build-tools
  {"lein"       "2.9.5"
   "boot"       "2.8.3"
   "tools-deps" "1.10.1.754"})

(def exclusions ; don't build these for whatever reason(s)
  #{{:jdk-version 8
     :distro      "alpine"}
    {:jdk-version 11
     :distro      "alpine"}
    {:jdk-version 15
     :distro      "alpine"}})

(def maintainers
  "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>")

(defn default-distro [jdk-version]
  (get default-distros jdk-version (:default default-distros)))

(defn contains-every-key-value?
  "Returns true if the map `haystack` contains every key-value pair in the map
  `needles`. `haystack` may contain additional keys that are not in `needles`.
  Returns false if any of the keys in `needles` are missing from `haystack` or
  have different values."
  [haystack needles]
  (every? (fn [[k v]]
            (= v (get haystack k)))
          needles))

(defn base-image-name [jdk-version distro]
  (str base-image ":" jdk-version "-" distro))

(defn exclude?
  "Returns true if `variant` matches one of `exclusions` elements (meaning
  `(contains-every-key-value? variant exclusion)` returns true)."
  [exclusions variant]
  (some (partial contains-every-key-value? variant) exclusions))

(defn docker-tag [{:keys [jdk-version distro build-tool build-tool-version]}]
  (if (= ::all build-tool)
    "latest"
    (let [jdk-label (if (= default-jdk-version jdk-version)
                      nil
                      (str base-image "-" jdk-version))
          dd (default-distro jdk-version)
          distro-label (if (= dd distro) nil distro)]
      (str/join "-" (remove nil? [jdk-label build-tool build-tool-version
                                  distro-label])))))

(s/def ::variant
  (s/keys :req-un [::jdk-version ::base-image ::distro ::build-tool
                   ::build-tool-version ::maintainer ::docker-tag]
          :opt-un [::build-tool-versions]))

(defn assoc-if [m pred k v]
  (if (pred)
    (assoc m k v)
    m))

(defn variant-map [[jdk-version distro [build-tool build-tool-version]]]
  (let [base {:jdk-version        jdk-version
              :base-image         (base-image-name jdk-version distro)
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
        build-cmd ["docker" "build" "--no-cache" "-t" image-tag "-f" dockerfile "."]]
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
  (list default-jdk-version (default-distro default-jdk-version) [::all]))

(defn image-variants [jdk-versions distros build-tools]
  (->> (combo/cartesian-product jdk-versions distros build-tools)
       (cons latest-variant)
       (map variant-map)
       (remove #(= ::s/invalid (s/conform ::variant %)))
       set))

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

(defn generate-dockerfiles! []
  (for [variant (image-variants jdk-versions distros build-tools)
        :when (not (exclude? exclusions variant))]
    (generate-dockerfile! variant)))

(defn -main [& args]
  (case (first args)
    "clean" (df/clean-all)
    "dockerfiles" (dorun (generate-dockerfiles!))
    (build-images (generate-dockerfiles!)))
  (System/exit 0))
