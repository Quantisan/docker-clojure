(ns docker-clojure.core
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.math.combinatorics :as combo]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [docker-clojure.dockerfile :as df]))

(s/def ::non-blank-string
  (s/and string? #(not (str/blank? %))))

(s/def ::base-image ::non-blank-string)
(s/def ::base-images (s/coll-of ::base-image :distinct true :into #{}))

(s/def ::distro ::non-blank-string)
(s/def ::distros (s/coll-of ::distro :distinct true :into #{}))

(s/def ::build-tool ::non-blank-string)
(s/def ::build-tool-version
  (s/and ::non-blank-string #(re-matches #"[\d\.]+" %)))
(s/def ::build-tools (s/map-of ::build-tool ::build-tool-version))

(s/def ::exclusions
  (s/keys :opt-un [::base-image ::distro ::build-tool ::build-tool-version]))

(s/def ::maintainers
  (s/map-of keyword? ::non-blank-string))

(def base-images
  #{"openjdk:8" "openjdk:11"})

(def distros
  #{"debian" "alpine"})

(def build-tools
  {"lein"       "2.8.1"
   "boot"       "2.8.1"
   "tools-deps" "1.9.0.397"})

(def exclusions ; don't build these for whatever reason(s)
  #{{:base-image "openjdk:11"
     :distro     "alpine"}})

(def maintainers
  {:paul "Paul Lam <paul@quantisan.com>"
   :wes  "Wes Morgan <wesmorgan@icloud.com>"
   :dlg  "Kirill Chernyshov <delaguardo@gmail.com>"})

(defn maintainer [{:keys [distro build-tool]}]
  (cond
    (and (= distro "debian") (= build-tool "lein"))
    (:paul maintainers)

    (and (= distro "debian") (= build-tool "boot"))
    (:wes maintainers)

    (= build-tool "tools-deps")
    (:dlg maintainers)

    (= distro "alpine")
    (:wes maintainers)))

(defn contains-every-key-value?
  "Returns true if the map `haystack` contains every key-value pair in the map
  `needles`. `haystack` may contain additional keys that are not in `needles`.
  Returns false if any of the keys in `needles` are missing from `haystack` or
  have different values."
  [haystack needles]
  (every? (fn [[k v]]
            (= v (get haystack k)))
          needles))

(defn exclude?
  "Returns true if `variant` matches one of `exclusions` elements (meaning
  `(contains-every-key-value? variant exclusion)` returns true)."
  [exclusions variant]
  (some (partial contains-every-key-value? variant) exclusions))

(defn base-image->tag-component [base-image]
  (str/replace base-image ":" "-"))

(defn docker-tag [{:keys [base-image distro build-tool]}]
  (let [build-tool-version (get build-tools build-tool)
        jdk-label (if (= "openjdk:8" base-image)
                    nil
                    (base-image->tag-component base-image))
        distro-label (if (= "debian" distro) nil distro)]
    (str/join "-" (remove nil? [jdk-label build-tool build-tool-version
                                distro-label]))))

(s/def ::variant
  (s/keys :req-un [::base-image ::distro ::build-tool ::build-tool-version
                   ::maintainer ::docker-tag]))

(defn variant-map [[base-image distro build-tool]]
  (let [base {:base-image base-image
              :distro     distro
              :build-tool build-tool}]
    (-> base
        (assoc :maintainer (maintainer base))
        (assoc :docker-tag (docker-tag base))
        (assoc :build-tool-version (get build-tools (:build-tool base))))))

(defn build-image [{:keys [docker-tag dockerfile] :as variant}]
  (let [image-tag (str "clojure:" docker-tag)
        build-cmd ["docker" "build" "-t" image-tag "-f"
                   dockerfile "empty"]]
    (df/write-file dockerfile variant)
    (apply println "Running" build-cmd)
    (let [{:keys [out err exit]} (apply sh build-cmd)]
      (if (zero? exit)
        (println "Succeeded")
        (do
          (println "ERROR:" err)
          (print out)))))
  (println))

(defn image-variants [base-images distros build-tools]
  (->> (combo/cartesian-product base-images distros build-tools)
       (map variant-map)
       (remove #(= ::s/invalid (s/conform ::variant %)))
       set))

(defn build-images [variants]
  (println "Building images")
  (doseq [variant variants]
    (when-not (exclude? exclusions variant)
      (build-image variant))))

(defn generate-dockerfile! [variant]
  (let [filename (df/filename variant)]
    (println "Generating" filename)
    (df/write-file filename variant)
    (assoc variant :dockerfile filename)))

(defn generate-dockerfiles! []
  (for [variant (image-variants base-images distros (keys build-tools))
        :when (not (exclude? exclusions variant))]
    (generate-dockerfile! variant)))

(defn -main [& args]
  (case (first args)
    "clean" (df/clean-all)
    "dockerfiles" (dorun (generate-dockerfiles!))
    (build-images (generate-dockerfiles!)))
  (System/exit 0))

