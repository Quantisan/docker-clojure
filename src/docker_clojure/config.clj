(ns docker-clojure.config
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [com.gfredericks.test.chuck.generators :as gen']
            [docker-clojure.core :as-alias core]))

(s/def ::non-blank-string
  (s/and string? #(not (str/blank? %))))

(s/def ::jdk-version
  (s/and pos-int? #(<= 8 %)))
(s/def ::jdk-versions (s/coll-of ::jdk-version :distinct true :into #{}))

(s/def ::base-image ::non-blank-string)
(s/def ::base-images (s/coll-of ::base-image :distinct true :into #{}))

(def docker-image-name-re (re-pattern "[-\\w]+(?::[-\\w.]+)?"))

(s/def ::docker-image-name
  (s/with-gen
    (s/and ::non-blank-string
           #(re-matches docker-image-name-re %))
    #(gen'/string-from-regex docker-image-name-re)))

(def docker-tag-re (re-pattern "[-\\w.]+"))

(s/def ::docker-tag
  (s/with-gen
    (s/and ::non-blank-string
           #(re-matches docker-tag-re %))
    #(gen'/string-from-regex docker-tag-re)))

(s/def ::base-image-tag ::docker-image-name)

(def distro-component-re (re-pattern "[-_A-Za-z][-\\w.]+"))

(s/def ::distro
  (s/with-gen
    (s/and qualified-keyword?
           #(->> %
                 ((juxt namespace name))
                 ((fn [elements]
                    (every? (fn [e] (re-matches distro-component-re e))
                            elements)))))
    #(gen/fmap (fn [[namespace local]] (keyword namespace local))
               (gen/vector (gen'/string-from-regex distro-component-re) 2))))

(s/def ::distros (s/coll-of ::distro :distinct true :into #{}))

(s/def ::specific-build-tool #{"lein" "tools-deps"})
(s/def ::build-tool (s/or ::specific-tool ::specific-build-tool
                          ::all-tools #{::core/all}))
(s/def ::specific-build-tool-version
  (s/with-gen
    (s/and ::non-blank-string
           #(re-matches #"(?:\d+\.)+\d+" %))
    #(gen/fmap (fn [nums] (str/join "." nums))
               (gen/vector (gen/int) 2 4))))

(s/def ::build-tool-version
  (s/nilable ::specific-build-tool-version))

(s/def ::build-tool-versions
  (s/map-of ::specific-build-tool ::specific-build-tool-version))

(s/def ::maintainers
  (s/coll-of ::non-blank-string :distinct true :into #{}))
(s/def ::maintainer ::non-blank-string)

(s/def ::architecture ::non-blank-string)
(s/def ::architectures (s/coll-of ::architecture :distinct true :into #{}))

(def git-repo "https://github.com/Quantisan/docker-clojure.git")

(def jdk-versions #{8 11 17 21 24})

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
              :debian-slim/bullseye-slim :debian/bullseye
              :debian-slim/trixie-slim :debian/trixie}})

(def architectures
  #{"amd64" "arm64v8" "ppc64le" "riscv64" "s390x"})

(def default-distros
  "The default distro to use for tags that don't specify one, keyed by jdk-version.
  :default is a fallback for jdk versions not o/w specified."
  {8        :ubuntu/noble
   11       :ubuntu/noble
   17       :ubuntu/noble
   :default :debian/bookworm})

(def build-tools
  {"lein"       "2.12.0"
   "tools-deps" "1.12.3.1577"})

(def default-build-tool "tools-deps")

(def installer-hashes
  {"lein"       {"2.11.2" "28a1a62668c5f427b413a8677e376affaa995f023b1fcd06e2d4c98ac1df5f3e"
                 "2.12.0" "12a9c5e3a2471619ca3d64a7462f920fdf713ae8959eb4fcd6257c23332b5aa4"}
   "tools-deps" {"1.12.2.1571" "14c532e6e2dd760c7bf9d5fdd2044f4ea63e11c9126b89d4e120591352499fff"
                 "1.12.3.1577" "a55106244ca93ef7b61309e9dca4b248257685870824a8abe2efa706ede8241f"}})

(def exclusions ; don't build these for whatever reason(s)
  #{;; No more jammy builds for JDK 23+
    {:jdk-version #(>= % 23)
     :distro      :ubuntu/jammy}
    ;; No upstream ARM alpine images available before JDK 21
    {:jdk-version  #(< % 21)
     :architecture "arm64v8"
     :distro       :alpine/alpine}
    ;; Only build amd64 & arm64 architectures for alpine
    {:architecture #(not (#{"amd64" "arm64v8"} %))
     :distro       :alpine/alpine}
    ;; ppc64le needs Debian Bookworm or newer
    {:architecture "ppc64le"
     :distro       #(and (-> % namespace (str/starts-with? "debian"))
                         (-> % name (str/starts-with? "bullseye")))}
    ;; riscv64 is only supported for Java 17+
    {:architecture "riscv64"
     :jdk-version  #(< % 17)}
    ;; riscv64 isn't supported on Ubuntu Jammy
    {:architecture "riscv64"
     :distro       :ubuntu/jammy}
    ;; riscv64 needs Debian Trixie or newer
    {:architecture "riscv64"
     :distro       #(and (-> % namespace (str/starts-with? "debian"))
                         (let [n (name %)]
                           (or (str/starts-with? n "bullseye")
                               (str/starts-with? n "bookworm"))))}
    ;; s390x is only supported for Java 11+
    {:architecture "s390x"
     :jdk-version  #(< % 11)}
    ;; s390x needs Debian Bookworm or newer
    {:architecture "s390x"
     :distro       #(and (-> % namespace (str/starts-with? "debian"))
                         (-> % name (str/starts-with? "bullseye")))}})

(def maintainers
  ["Paul Lam <paul@quantisan.com> (@Quantisan)"
   "Wes Morgan <wes@wesmorgan.me> (@cap10morgan)"])
