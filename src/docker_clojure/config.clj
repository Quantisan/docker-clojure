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

(s/def ::maintainers
  (s/coll-of ::non-blank-string :distinct true :into #{}))

(s/def ::architecture ::non-blank-string)
(s/def ::architectures (s/coll-of ::architecture :distinct true :into #{}))

(def git-repo "https://github.com/Quantisan/docker-clojure.git")

(def jdk-versions #{8 11 17 21 22})

(def base-images
  "Map of JDK version to base image name(s) with :default as a fallback"
  {8        ["eclipse-temurin" "debian"]
   11       ["eclipse-temurin" "debian"]
   17       ["eclipse-temurin" "debian"]
   :default ["debian" "eclipse-temurin"]})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 21)

(def distros
  "Map of base image name to set of distro tags to use, namespaced by Linux
  distro type. :default key is a fallback for base images not o/w specified."
  {:default #{:alpine/alpine :ubuntu/jammy :ubuntu/noble}
   "debian" #{:debian-slim/bookworm-slim :debian/bookworm
              :debian-slim/bullseye-slim :debian/bullseye}})

(def default-architectures
  #{"amd64" "arm64v8"})

(def distro-architectures
  "Map of distro types to architectures it supports if different from
  default-architectures."
  {:alpine #{"amd64"}})

(def default-distros
  "The default distro to use for tags that don't specify one, keyed by jdk-version.
  :default is a fallback for jdk versions not o/w specified."
  {8        :ubuntu/jammy
   11       :ubuntu/jammy
   17       :ubuntu/jammy
   :default :debian/bookworm})

(def build-tools
  {"lein"       "2.11.2"
   "tools-deps" "1.11.3.1456"})

(def default-build-tool "tools-deps")

(def installer-hashes
  {"lein"       {"2.11.1" "03b3fbf7e6fac262f88f843a87b712a2b37f39cffc4f4f384436a30d8b01d6e4"
                 "2.11.2" "28a1a62668c5f427b413a8677e376affaa995f023b1fcd06e2d4c98ac1df5f3e"}
   "tools-deps" {"1.11.1.1435" "7edee5b12197a2dbe6338e672b109b18164cde84bea1f049ceceed41fc4dd10a"
                 "1.11.3.1456" "2f5edc801133c72a49e990816b0e245beb8b4e35a85524b4dd0b3fa03a4a5365"}})

(def exclusions ; don't build these for whatever reason(s)
  #{;; commented out example
    #_{:jdk-version 8
       :distro      :alpine/alpine}})

(def maintainers
  ["Paul Lam <paul@quantisan.com> (@Quantisan)"
   "Wes Morgan <wes@wesmorgan.me> (@cap10morgan)"])
