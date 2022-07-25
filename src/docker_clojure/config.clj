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
  {:default "eclipse-temurin"})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 17)

(def distros
  "Map of base image name to set of distro tags to use, namespaced by Linux
  distro type. :default key is a fallback for base images not o/w specified."
  {:default  #{:alpine/alpine :ubuntu/focal :ubuntu/jammy}})

(def default-architectures
  #{"amd64" "arm64v8"})

(def distro-architectures
  "Map of distro types to architectures it supports if different from
  default-architectures."
  {:alpine #{"amd64"}})

(def default-distros
  "The default distro to use for tags that don't specify one, keyed by jdk-version.
  :default is a fallback for jdk versions not o/w specified."
  {:default :ubuntu/jammy})

(def build-tools
  {"lein"       "2.9.8"
   "boot"       "2.8.3"
   "tools-deps" "1.11.1.1149"})

(def default-build-tool "tools-deps")

(def installer-hashes
  {"lein"       {"2.9.6" "094b58e2b13b42156aaf7d443ed5f6665aee27529d9512f8d7282baa3cc01429"
                 "2.9.7" "f78f20d1931f028270e77bc0f0c00a5a0efa4ecb7a5676304a34ae4f469e281d"
                 "2.9.8" "9952cba539cc6454c3b7385ebce57577087bf2b9001c3ab5c55d668d0aeff6e9"}
   "boot"       {"2.8.3" "0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3"}
   "tools-deps" {"1.11.1.1113" "7677bb1179ebb15ebf954a87bd1078f1c547673d946dadafd23ece8cd61f5a9f"
                 "1.11.1.1124" "9c7d226ae1c08b6dbb7f10c9ca1ab8c80f8d5021b6a1e535b5dd92ba3ff062db"
                 "1.11.1.1129" "02bec16a9bc0a7a1a7912ca72fc8042f46e106369e572ca7185a8522e8f40123"
                 "1.11.1.1139" "2ba0de34f122f26580e3e6337bd04c8e3f9f648299367b17da85f00d930e191c"
                 "1.11.1.1149" "9aadc1a1840a458517a6efb111eba72be93c17bbdc874c833ef781e77aacc55e"}})

(def exclusions ; don't build these for whatever reason(s)
  ;; commented out example
  #{#_{:jdk-version 8
       :distro      :alpine/alpine}})

(def maintainers
  ["Paul Lam <paul@quantisan.com> (@Quantisan)"
   "Wes Morgan <wes@wesmorgan.me> (@cap10morgan)"])
