(ns docker-clojure.core
  (:require
   [clojure.core.async :refer [<!! chan pipeline-blocking to-chan!] :as async]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [docker-clojure.config :as cfg]
   [docker-clojure.docker :as docker]
   [docker-clojure.dockerfile :as df]
   [docker-clojure.log :refer [log] :as logger]
   [docker-clojure.manifest :as manifest]
   [docker-clojure.util :refer [get-or-default]]
   [docker-clojure.variant :as variant]))

(defn exclude?
  "Returns true if `variant` matches one of `exclusions` elements (meaning
  `(contains-every-key-value? variant exclusion)` returns true)."
  [exclusions variant]
  (some (partial variant/exclude? variant) exclusions))

(def latest-variants
  "The latest variant is special because we include all 3 build tools via the
  [::all] value on the end."
  (for [arch cfg/architectures]
    (list (-> cfg/base-images :default first)
          cfg/default-jdk-version
          (get-or-default cfg/default-distros cfg/default-jdk-version)
          [::all]
          arch)))

(defn- invalid-variant?
  [variant]
  (let [status   (s/conform ::variant/variant variant)
        invalid? (= ::s/invalid status)]
    (when invalid?
      (println "invalid variant:" (pr-str variant))
      true)))

(defn image-variants
  [base-images jdk-versions distros build-tools architectures]
  (into #{}
        (comp
         (map variant/->map)
         (remove invalid-variant?))
        (concat
         (variant/combinations base-images jdk-versions distros build-tools
                               architectures)
         latest-variants)))

(defn rand-delay
  "Runs argument f w/ any supplied args after a random delay of 100-1000 ms"
  [f & args]
  (let [rand-time (+ 100 (rand-int 900))]
    (Thread/sleep rand-time)
    (apply f args)))

(defn build-images
  [parallelization installer-hashes variants]
  (log "Building images" parallelization "at a time")
  (let [variants-ch (to-chan! variants)
        builds-ch   (chan parallelization)]
    ;; Kick off builds with a random delay so we don't have Docker race
    ;; conditions (e.g. build container name collisions)
    (async/thread (pipeline-blocking parallelization builds-ch
                                     (map (partial rand-delay docker/build-image
                                                   installer-hashes))
                                     variants-ch))
    (while (<!! builds-ch))))

(defn generate-dockerfiles! [installer-hashes variants]
  (log "Generated" (count variants) "variants")
  (doseq [variant variants]
    (df/generate! installer-hashes variant)))

(defn valid-variants []
  (remove (partial exclude? cfg/exclusions)
          (image-variants cfg/base-images cfg/jdk-versions cfg/distros
                          cfg/build-tools cfg/architectures)))

(defn generate-manifest! [variants args]
  (let [git-head    (->> ["git" "rev-parse" "HEAD"] (apply sh) :out)
        target-file (or (first args) :stdout)
        manifest    (manifest/generate {:maintainers   cfg/maintainers
                                        :architectures cfg/architectures
                                        :git-repo      cfg/git-repo}
                                       git-head variants)]
    (log "Writing manifest of" (count variants) "variants to" target-file "...")
    (let [output-writer (if (= :stdout target-file)
                          *out*
                          (io/writer target-file))]
      (.write output-writer manifest)
      (when (not= :stdout target-file)
        (.close output-writer)))))

(defn generate-variants
  [args]
  ; TODO: Maybe replace this with bb/cli
  (let [key-vals       (->> args
                            (map #(if (str/starts-with? % ":")
                                    (edn/read-string %)
                                    %))
                            (map #(try (Integer/parseInt %)
                                       (catch Exception _ %)))
                            (partition 2))
        variant-filter #(or
                         (empty? key-vals)
                         (every? (fn [[k v]]
                                   (= (get % k) v))
                                 key-vals))]
    (when (seq key-vals)
      (println "Filtering variants with:")
      (doseq [[k v] key-vals]
        (println (str "(= " (pr-str v) " (get variant " (pr-str k) "))")))
      (println))
    (filter variant-filter (valid-variants))))

(defn run
  "Entrypoint for exec-fn."
  [{:keys [cmd args parallelization]}]
  (logger/start)
  (let [variants (generate-variants args)]
    (case cmd
      :clean (df/clean-all)
      :dockerfiles (generate-dockerfiles! cfg/installer-hashes variants)
      :manifest (generate-manifest! variants args)
      :build-images (build-images parallelization cfg/installer-hashes variants)))
  (logger/stop))

(defn -main
  [& cmd-args]
  (let [[cmd & args] cmd-args]
    (run {:cmd             (if cmd (keyword cmd) :build-images)
          :args            args
          :parallelization 4})))
