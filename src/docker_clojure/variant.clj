(ns docker-clojure.variant
  (:refer-clojure :exclude [compare sort])
  (:require [clojure.math.combinatorics :as combo]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [docker-clojure.config :as cfg]
            [docker-clojure.core :as-alias core]
            [docker-clojure.docker :as docker]
            [docker-clojure.util :refer [get-or-default]]))

(s/def ::variant-base
  (s/keys :req-un [::cfg/jdk-version ::cfg/base-image ::cfg/base-image-tag
                   ::cfg/distro ::cfg/build-tool ::cfg/build-tool-version
                   ::cfg/maintainer ::cfg/docker-tag ::cfg/architecture]
          :opt-un [::cfg/build-tool-versions]))

(s/def ::variant
  (s/with-gen
   ::variant-base
   #(gen/fmap (fn [[v btv]]
                (if (= ::core/all (:build-tool v))
                  (-> v ; ::core/all implies docker tag "latest"
                      (assoc :build-tool-version nil
                             :build-tool-versions btv)
                      (dissoc :distro :docker-tag :base-image-tag :base-image))
                  v))
              (gen/tuple (s/gen ::variant-base)
                         (gen/map (s/gen ::cfg/specific-build-tool)
                                  (s/gen ::cfg/specific-build-tool-version))))))

(s/def ::variants (s/coll-of ::variant))

(s/def ::manifest-variant
  (s/keys :req-un [::cfg/jdk-version ::cfg/base-image ::cfg/base-image-tag
                   ::cfg/distro ::cfg/build-tool ::cfg/build-tool-version
                   ::cfg/maintainer ::cfg/docker-tag]
          :opt-un [::cfg/build-tool-versions ::cfg/architectures]))

(defn assoc-if
  [m pred k v]
  (if (pred)
    (assoc m k v)
    m))

(defn ->map [[base-image jdk-version distro
              [build-tool build-tool-version] architecture]]
  (let [base {:jdk-version        jdk-version
              :architecture       architecture
              :base-image         base-image
              :base-image-tag     (docker/base-image-tag base-image jdk-version
                                                         distro)
              :distro             distro
              :build-tool         build-tool
              :build-tool-version build-tool-version
              :maintainer         (str/join " & " cfg/maintainers)}]
    (-> base
        (assoc :docker-tag (docker/default-tag base))
        (assoc-if #(nil? (:build-tool-version base)) :build-tool-versions
                  cfg/build-tools))))

(s/def ::variant-tuple
  (s/tuple ::cfg/base-image ::cfg/jdk-version ::cfg/distro
           (s/tuple ::cfg/specific-build-tool ::cfg/specific-build-tool-version)
           ::cfg/architecture))

(s/fdef ->map
  :args (s/cat :variant-tuple ::variant-tuple)
  :ret  ::variant
  :fn   #(let [[base-image jdk-version distro
                [specific-build-tool specific-build-tool-version]
                architecture] (-> % :args :variant-tuple)]
           (println "arg:" (-> % :args :variant-tuple pr-str))
           (println "ret:" (-> % :ret pr-str))
           (and (= (-> % :ret :base-image) base-image)
                (= (-> % :ret :jdk-version) jdk-version)
                (= (-> % :ret :distro) distro)
                (= (-> % :ret :base-image-tag)
                   (docker/base-image-tag base-image jdk-version distro))
                (= (-> % :ret :build-tool last) specific-build-tool)
                (= (-> % :ret :build-tool-version) specific-build-tool-version)
                (= (-> % :ret :architecture) architecture))))

(defn exclude?
  "Returns true if the map `variant` contains every key-value pair in the map
  `exclusion`. `variant` may contain additional keys that are not in
  `exclusion`. Some values of `exclusion` can also be a predicate of one
  argument which is then tested against the respective value from `variant`.
  Returns false if any of the keys in `exclusions` are missing from `variant` or
  have different values, or the predicate value returned false."
  [variant exclusion]
  (every? (fn [[k v]]
            (if (fn? v)
              (v (get variant k))
              (= v (get variant k))))
          exclusion))

(defn compare
  [v1 v2]
  (let [c (clojure.core/compare (:jdk-version v1) (:jdk-version v2))]
    (if (not= c 0)
      c
      (let [c (clojure.core/compare (docker/full-tag v1)
                                    (docker/full-tag v2))]
        (if (not= c 0)
          c
          (clojure.core/compare (:architecture v1) (:architecture v2)))))))

(defn sort
  [variants]
  (clojure.core/sort
   (fn [v1 v2]
     (cond
       (= "latest" (:docker-tag v1)) -1
       (= "latest" (:docker-tag v2)) 1
       :else (compare v1 v2)))
   variants))

(defn equal?
  [v1 v2]
  (= 0 (compare v1 v2)))

(defn equal-except-architecture?
  [{arch1 :architecture :as v1} {arch2 :architecture :as v2}]
  (and (not= arch1 arch2)
       (equal? (dissoc v1 :architecture) (dissoc v2 :architecture))))

(defn combinations
  [base-images jdk-versions distros build-tools architectures]
  (reduce
   (fn [variants jdk-version]
     (concat
      variants
      (let [jdk-base-images (get-or-default base-images jdk-version)]
        (loop [[bi & r] jdk-base-images
               acc #{}]
          (let [vs   (combo/cartesian-product #{bi} #{jdk-version}
                                              (get-or-default distros bi)
                                              build-tools
                                              architectures)
                acc' (concat acc vs)]
            (if (seq r)
              (recur r acc')
              acc'))))))
   #{} jdk-versions))

(defn merge-architectures
  [default-architectures variants]
  (->> variants
       (map #(assoc % :architectures #{(:architecture %)}))
       (reduce
        (fn [mav v]
          (if-let [matching
                   (some #(when
                           (equal-except-architecture? v %)
                           %)
                         mav)]
            (-> mav
                (->> (remove #(= % matching)))
                (conj (update matching :architectures conj
                              (:architecture v))))
            (conj mav v)))
        [])
       (map #(if (= (:architectures %) default-architectures)
               (dissoc % :architectures :architecture)
               (dissoc % :architecture)))
       sort))

(s/fdef merge-architectures
  :args (s/cat :default-architectures ::cfg/architectures
               :variants
               (s/with-gen
                ::variants
                #(gen/fmap
                  (fn [variants]
                    ;; duplicate variants for each architecture
                    (mapcat (fn [variant]
                              (map (fn [arch]
                                     (assoc variant :architecture arch))
                                   cfg/architectures))
                            variants))
                  (s/gen (s/coll-of ::variant)))))
  :ret  (s/coll-of ::manifest-variant)
  :fn   #(let [ret-count        (-> % :ret count)
               arg-variants     (-> % :args :variants)
               ;; Examine the return value to see how many unique variants we have
               ;; after merging all architectures
               variant-keys     (-> arg-variants first keys set
                                    (disj :architecture))
               unique-variants  (->> arg-variants
                                     (map (fn [v] (select-keys v variant-keys)))
                                     set count)]
           ;; We expect to have one merged variant for each unique combination of keys
           ;; other than architecture
           (= ret-count unique-variants)))
