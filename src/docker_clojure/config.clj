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

(def jdk-versions #{8 11 17 19})

(def base-images
  "Map of JDK version to base image name with :default as a fallback"
  {:default "eclipse-temurin"})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 17)

(def distros
  "Map of base image name to set of distro tags to use, namespaced by Linux
  distro type. :default key is a fallback for base images not o/w specified."
  {:default #{:alpine/alpine :ubuntu/focal :ubuntu/jammy}})

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
  {"lein"       "2.9.10"
   "boot"       "2.8.3"
   "tools-deps" "1.11.1.1165"})

(def default-build-tool "tools-deps")

(def installer-hashes
  {"lein"       {"2.9.10" "dbb84d13d6df5b85bbf7f89a39daeed103133c24a4686d037fe6bd65e38e7f32"}
   "boot"       {"2.8.3" "0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3"}
   "tools-deps" {"1.11.1.1149" "9aadc1a1840a458517a6efb111eba72be93c17bbdc874c833ef781e77aacc55e"
                 "1.11.1.1155" "7eb9aa2ecc6c0abfdb1578d4b99ca7c2055111aafa38524a12a6fb76fe01f30b"
                 "1.11.1.1165" "72d662bdc99b79037f9e34996272384de35e01e0416d8eb79cc940ee0f0fc808"}})

(def exclusions ; don't build these for whatever reason(s)
  ;; commented out example
  #{#_{:jdk-version 8
       :distro      :alpine/alpine}})

(def maintainers
  ["Paul Lam <paul@quantisan.com> (@Quantisan)"
   "Wes Morgan <wes@wesmorgan.me> (@cap10morgan)"])
