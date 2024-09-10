(ns docker-clojure.variant
  (:refer-clojure :exclude [compare sort])
  (:require [clojure.math.combinatorics :as combo]
            [clojure.string :as str]
            [docker-clojure.config :as cfg]
            [docker-clojure.docker :as docker]
            [docker-clojure.util :refer [get-or-default]]))

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
  [v1 v2]
  (= 0 (compare (dissoc v1 :architecture) (dissoc v2 :architecture))))

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
  [variants default-architectures]
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
