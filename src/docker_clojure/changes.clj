(ns docker-clojure.changes
  "Works out which build dirs a change to the generated Dockerfiles touches and
  which of them are worth building to verify it."
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [docker-clojure.config :as cfg]
            [docker-clojure.dockerfile :as df]
            [docker-clojure.util :refer [get-or-default]]))

(defn- parse-file-diff
  "Parses one file's section of a git diff into its build dir, file name,
  status and changed lines. Returns nil for files that aren't in a build dir."
  [[header & lines]]
  (let [[_ path] (re-matches #"diff --git a/(.+) b/\1" header)
        parts    (some-> path (str/split #"/"))]
    (when (and (= 4 (count parts)) (= "target" (first parts)))
      {:build-dir (str/join "/" (butlast parts))
       :file      (last parts)
       :status    (cond
                    (some #(str/starts-with? % "new file mode") lines) :added
                    (some #(str/starts-with? % "deleted file mode") lines) :deleted
                    :else :modified)
       :lines     (->> lines
                       (drop-while #(not (str/starts-with? % "@@")))
                       (filter #(re-find #"^[+-]" %)))})))

(defn parse-diff
  "Parses `git diff -U0 --no-renames` output into a map of build dir to its
  change: `:added?` is true for new build dirs and `:signature` is equal for
  build dirs that received exactly the same change. Deleted build dirs are
  left out."
  [diff]
  (->> (str/split-lines diff)
       (partition-by #(str/starts-with? % "diff --git "))
       (partition 2)
       (keep (fn [[header lines]] (parse-file-diff (concat header lines))))
       (group-by :build-dir)
       (keep (fn [[build-dir files]]
               (let [dockerfile (some #(when (= "Dockerfile" (:file %)) %)
                                      files)]
                 (when-not (= :deleted (:status dockerfile))
                   [build-dir
                    {:added?    (= :added (:status dockerfile))
                     :signature (set (map (juxt :file :status :lines)
                                          files))}]))))
       (into {})))

(defn- preference
  "Sort key that puts the default JDK version on its default distro with the
  default build tool first."
  [[build-dir {:keys [jdk-version distro build-tool]}]]
  [(not= cfg/default-jdk-version jdk-version)
   (not= (get-or-default cfg/default-distros jdk-version) distro)
   (not= cfg/default-build-tool build-tool)
   build-dir])

(defn- traits
  "What building a build dir's variant exercises: its JDK version on its
  distro (i.e. its base image) and its build tool."
  [[_build-dir {:keys [jdk-version distro build-tool]}]]
  #{[:base jdk-version distro] [:build-tool build-tool]})

(defn- covering
  "Greedily picks from `dir-variants` until every JDK version & distro pair
  and every build tool among them has been picked at least once."
  [dir-variants]
  (loop [candidates (sort-by preference dir-variants)
         uncovered  (into #{} (mapcat traits) dir-variants)
         picked     []]
    (if (empty? uncovered)
      picked
      (let [gain #(count (set/intersection uncovered (traits %)))
            best (first (sort-by (comp - gain) candidates))]
        (recur (remove #{best} candidates)
               (set/difference uncovered (traits best))
               (conj picked best))))))

(defn select-build-dirs
  "Returns the sorted build dirs worth building to verify `changes` (see
  `parse-diff`). Of the new build dirs, just enough are built to exercise each
  of their JDK version & distro pairs and build tools once. Modified build dirs
  that received exactly the same change are represented by one of them. Both
  prefer the default JDK version on its default distro."
  [variants changes]
  (let [dir->variant (into {} (map (juxt df/build-dir identity)) variants)
        dir-variants (fn [dir-changes]
                       (map (fn [[build-dir _]]
                              [build-dir (dir->variant build-dir)])
                            dir-changes))
        known        (filter (comp dir->variant key) changes)
        added        (filter (comp :added? val) known)
        modified     (remove (comp :added? val) known)]
    (->> (group-by (comp :signature val) modified)
         vals
         (map #(first (sort-by preference (dir-variants %))))
         (concat (covering (dir-variants added)))
         (map first)
         sort)))
