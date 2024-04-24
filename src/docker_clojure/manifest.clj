(ns docker-clojure.manifest
  (:require [clojure.string :as str]
            [docker-clojure.dockerfile :as df]
            [docker-clojure.util :refer [docker-tag full-docker-tag]]))

(defn variant-tags
  "Generates all the Docker Hub tag variations for the given variant"
  [variant]
  (->> [[]                                           ; temurin-21-lein-2.11-bookworm
        [:omit-jdk?]                                 ; lein-2.11-bookworm
        [:omit-distro?]                              ; temurin-21-lein-2.11
        [:omit-build-tool?]                          ; temurin-21-bookworm
        [:omit-build-tool-version?]                  ; temurin-21-lein-bookworm
        [:omit-jdk? :omit-distro?]                   ; lein-2.11
        [:omit-jdk? :omit-build-tool-version?]       ; lein-bookworm
        [:omit-distro? :omit-build-tool]             ; temurin-21
        [:omit-distro? :omit-build-tool-version?]    ; temurin-21-lein
        [:omit-jdk? :omit-distro? :omit-build-tool?] ; latest
        ]
       (map #(docker-tag (zipmap % (repeat true)) variant))
       distinct
       sort))

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
