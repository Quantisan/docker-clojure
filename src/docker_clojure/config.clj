(ns docker-clojure.config
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [docker-clojure.core :as-alias core]))

(s/def ::non-blank-string
  (s/and string? #(not (str/blank? %))))

(s/def ::jdk-version
  (s/and pos-int? #(<= 8 %)))
(s/def ::jdk-versions (s/coll-of ::jdk-version :distinct true :into #{}))

(s/def ::base-image ::non-blank-string)
(s/def ::base-images (s/coll-of ::base-image :distinct true :into #{}))

(s/def ::docker-image-name (s/and ::non-blank-string
                                  #(re-matches #"[-\w]+(?::[-\w.]+)?" %)))
(s/def ::docker-tag (s/and ::non-blank-string
                           #(re-matches #"[-\w.]+" %)))
(s/def ::base-image-tag ::docker-image-name)

(s/def ::distro qualified-keyword?)
(s/def ::distros (s/coll-of ::distro :distinct true :into #{}))

(s/def ::build-tool (s/or ::specific-tool ::non-blank-string
                          ::all-tools #(= ::core/all %)))
(s/def ::build-tool-version
  (s/nilable (s/and ::non-blank-string #(re-matches #"[\d\.]+" %))))
(s/def ::build-tools (s/map-of ::build-tool ::build-tool-version))

(s/def ::exclusions
  (s/keys :opt-un [::jdk-version ::distro ::build-tool ::build-tool-version]))

(s/def ::maintainers
  (s/coll-of ::non-blank-string :distinct true :into #{}))

(s/def ::architecture ::non-blank-string)
(s/def ::architectures (s/coll-of ::architecture :distinct true :into #{}))

(def git-repo "https://github.com/Quantisan/docker-clojure.git")

(def jdk-versions #{8 11 17 18})

(def base-images
  "Map of JDK version to base image name with :default as a fallback"
  {8        "openjdk"
   11       "openjdk"
   :default "eclipse-temurin"})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 17)

(def distros
  "Map of base image name to set of distro tags to use, namespaced by Linux
  distro type. :default key is a fallback for base images not o/w specified."
  {"openjdk" #{:debian/buster :debian-slim/slim-buster :debian/bullseye
               :debian-slim/slim-bullseye :alpine/alpine}
   :default  #{:alpine/alpine :ubuntu/focal}})

(def default-architectures
  #{"amd64" "arm64v8"})

(def distro-architectures
  "Map of distro types to architectures it supports if different from
  default-architectures."
  {:alpine #{"amd64"}})

(def default-distros
  "The default distro to use for tags that don't specify one, keyed by jdk-version.
  :default is a fallback for jdk versions not o/w specified."
  {8        :debian-slim/slim-bullseye
   11       :debian-slim/slim-bullseye
   :default :ubuntu/focal})

(def build-tools
  {"lein"       "2.9.8"
   "boot"       "2.8.3"
   "tools-deps" "1.11.1.1113"})

(def default-build-tool "tools-deps")

(def installer-hashes
  {"lein"       {"2.9.6" "094b58e2b13b42156aaf7d443ed5f6665aee27529d9512f8d7282baa3cc01429"
                 "2.9.7" "f78f20d1931f028270e77bc0f0c00a5a0efa4ecb7a5676304a34ae4f469e281d"
                 "2.9.8" "9952cba539cc6454c3b7385ebce57577087bf2b9001c3ab5c55d668d0aeff6e9"}
   "boot"       {"2.8.3" "0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3"}
   "tools-deps" {"1.11.0.1100" "a71bd520bd43d4be6e0cab0c525f5d1f85911fc276f3d0f37f00243fb0f1e594"
                 "1.11.1.1105" "5655c3ee3ea495d0778d8a87ce05a719045d3ceae9dd5cc29033379d8f82cce5"
                 "1.11.1.1113" "7677bb1179ebb15ebf954a87bd1078f1c547673d946dadafd23ece8cd61f5a9f"}})

(def exclusions ; don't build these for whatever reason(s)
  #{{:jdk-version 8
     :distro      :alpine/alpine}
    {:jdk-version 11
     :distro      :alpine/alpine}})

(def maintainers
  ["Paul Lam <paul@quantisan.com> (@Quantisan)"
   "Wes Morgan <wes@wesmorgan.me> (@cap10morgan)"])
