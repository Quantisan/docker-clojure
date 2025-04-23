(ns docker-clojure.manifest
  (:require [clojure.string :as str]
            [docker-clojure.docker :as docker]
            [docker-clojure.dockerfile :as df]
            [docker-clojure.variant :as variant]))

(defn variant->manifest
  [variant]
  (str/join "\n"
            (conj
              (remove nil? [(str/join " " (conj ["Tags:"]
                                                (->> variant
                                                     docker/all-tags
                                                     (str/join ", "))))
                            (when-let [arch (:architectures variant)]
                              (str/join " " ["Architectures:"
                                             (str/join ", " arch)]))
                            (str/join " " ["Directory:"
                                           (df/build-dir variant)])])
              nil)))

(defn generate
  "Generates Docker manifest file for a given git commit and returns it as a
  string."
  [{:keys [maintainers architectures git-repo]} git-commit variants]
  (let [merged-arch-variants (variant/merge-architectures architectures variants)
        maintainers-label "Maintainers:"
        maintainers-sep (apply str ",\n" (repeat (inc (count maintainers-label))
                                                 " "))]
    (str/join "\n"
              (concat
                [(str/join " " [maintainers-label
                                (str/join maintainers-sep maintainers)])
                 (str/join " " ["Architectures:" (str/join ", " architectures)])
                 (str/join " " ["GitRepo:" git-repo])
                 (str/join " " ["GitCommit:" git-commit])]

                (map variant->manifest merged-arch-variants)

                [nil]))))
