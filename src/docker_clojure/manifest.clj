(ns docker-clojure.manifest
  (:require [clojure.string :as str]
            [docker-clojure.dockerfile :as df]
            [docker-clojure.util :refer [docker-tag full-docker-tag]]))

(defn variant-tags
  "Generates all the Docker Hub tag variations for the given variant"
  [variant]
  (let [short-tag (:docker-tag variant)
        full-tag  (full-docker-tag variant)
        base      (into #{} [short-tag full-tag])]
    (-> base
        (conj
          (docker-tag {:omit-jdk? true} variant)
          (docker-tag {:omit-build-tool? true} variant)
          (docker-tag {:omit-build-tool-version? true} variant)
          (docker-tag {:omit-distro? true} variant)
          (docker-tag {:omit-jdk? true, :omit-distro? true
                       :omit-build-tool-version? true} variant))
        vec
        sort)))

(defn variant->manifest
  [variant]
  (str/join "\n"
            (conj
              (remove nil? [(str/join " " (conj ["Tags:"]
                                                (->> variant
                                                     variant-tags
                                                     (str/join ", "))))
                            (when-let [arch (:architectures variant)]
                              (str/join " " ["Architectures:" (str/join ", " arch)]))
                            (str/join " " ["Directory:" (df/build-dir variant)])])
              nil)))

(defn generate
  "Generates Docker manifest file for a given git commit and returns it as a
  string."
  [{:keys [maintainers architectures git-repo]} git-commit variants]
  (let [maintainers-label "Maintainers:"
        maintainers-sep (apply str ",\n" (repeat (inc (count maintainers-label)) " "))]
    (str/join "\n"
              (concat
                [(str/join " " [maintainers-label
                                (str/join maintainers-sep maintainers)])
                 (str/join " " ["Architectures:" (str/join ", " architectures)])
                 (str/join " " ["GitRepo:" git-repo])
                 (str/join " " ["GitCommit:" git-commit])]

                (map variant->manifest variants)

                [nil]))))

