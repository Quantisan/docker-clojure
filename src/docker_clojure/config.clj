(ns docker-clojure.config
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [com.gfredericks.test.chuck.generators :as gen']
            [docker-clojure.core :as-alias core]))

(s/def ::non-blank-string
  (s/with-gen
    (s/and string? #(not (str/blank? %)))
    ;; Generate non-blank by construction (the default string generator yields
    ;; "" at size 0, which the not-blank such-that can't satisfy), and with
    ;; enough length/entropy that `:distinct true` collections of these (e.g.
    ;; ::architectures) don't collide and starve their distinctness such-that.
    #(gen'/string-from-regex #"[A-Za-z0-9]{8,32}")))

(s/def ::jdk-version
  (s/with-gen
    (s/and pos-int? #(<= 8 %))
    ;; Generate in-range by construction; the default pos-int generator yields
    ;; values < 8 the >= 8 such-that can't satisfy at small sizes (flaky gen).
    #(gen/choose 8 30)))
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

(def specific-build-tools #{"lein" "tools-deps"})
(s/def ::specific-build-tool specific-build-tools)
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
  (s/with-gen
    (s/map-of ::specific-build-tool ::specific-build-tool-version)
    ;; Build the map by construction rather than via gen/map over the tiny
    ;; #{"lein" "tools-deps"} key domain, which occasionally targets >2 distinct
    ;; keys and starves its such-that.
    #(gen/fmap (fn [versions] (zipmap specific-build-tools versions))
               (gen/vector (s/gen ::specific-build-tool-version)
                           (count specific-build-tools)))))

(s/def ::maintainers
  (s/coll-of ::non-blank-string :distinct true :into #{}))
(s/def ::maintainer ::non-blank-string)

(s/def ::architecture ::non-blank-string)
(s/def ::architectures (s/coll-of ::architecture :distinct true :into #{}))

(def git-repo "https://github.com/Quantisan/docker-clojure.git")

(def jdk-versions #{8 11 17 21 25 26})

(def base-images
  "Map of JDK version to base image name(s) with :default as a fallback"
  {8        ["eclipse-temurin" "debian"]
   11       ["eclipse-temurin" "debian"]
   17       ["eclipse-temurin" "debian"]
   :default ["debian" "eclipse-temurin"]})

;; The default JDK version to use for tags that don't specify one; usually the latest LTS release
(def default-jdk-version 25)

(def distros
  "Map of base image name to set of distro tags to use, namespaced by Linux
  distro type. :default key is a fallback for base images not o/w specified."
  {:default #{:alpine/alpine :ubuntu/jammy :ubuntu/noble}
   "debian" #{:debian-slim/bookworm-slim :debian/bookworm
              :debian-slim/bullseye-slim :debian/bullseye
              :debian-slim/trixie-slim :debian/trixie}})

(def architectures
  #{"amd64" "arm64v8" "ppc64le"})

(def default-distros
  "The default distro to use for tags that don't specify one, keyed by jdk-version.
  :default is a fallback for jdk versions not o/w specified."
  {8        :ubuntu/noble
   11       :ubuntu/noble
   17       :ubuntu/noble
   :default :debian/bookworm})

(def build-tools
  {"lein"       "2.13.0"
   "tools-deps" "1.12.6.1673"})

(def default-build-tool "tools-deps")

(def installer-hashes
  {"tools-deps" {"1.12.5.1664" "fb2f0ce23373d64bb4f13fce2ce2924c54ee0c033755357900808a1250621d82"
                 "1.12.6.1673" "5ae63b082ed33bf4c29bf1a8317c5c15249d1bc753676b2f5177fb3804ad6f77"}})

(def exclusions ; don't build these for whatever reason(s)
  #{;; Leiningen 2.13.0+ requires Java 11+
    {:jdk-version #(< % 11)
     :build-tool  "lein"}
    ;; No more jammy builds for JDK 23+
    {:jdk-version #(>= % 23)
     :distro      :ubuntu/jammy}
    ;; No upstream ARM alpine images available before JDK 21
    {:jdk-version  #(< % 21)
     :architecture "arm64v8"
     :distro       :alpine/alpine}
    ;; Only build amd64 & arm64 architectures for alpine
    {:architecture #(not (#{"amd64" "arm64v8"} %))
     :distro       :alpine/alpine}
    ;; Alpine w/ Java 8 stopped building correctly and not worth the time to fix
    {:jdk-version 8
     :distro      :alpine/alpine}
    ;; ppc64le needs Debian Bookworm or newer
    {:architecture "ppc64le"
     :distro       #(and (-> % namespace (str/starts-with? "debian"))
                         (-> % name (str/starts-with? "bullseye")))}})

(def maintainers
  ["Paul Lam <paul@quantisan.com> (@Quantisan)"
   "Wes Morgan <wes@wesmorgan.me> (@cap10morgan)"])
