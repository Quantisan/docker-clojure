(ns docker-clojure.core-test
  (:require [clojure.test :refer [deftest is are testing]]
            [docker-clojure.core :refer [exclude? image-variants]]
            [docker-clojure.config :as cfg]))

(deftest image-variants-test
  (testing "generates the expected set of variants"
    (with-redefs [cfg/default-distros     {8        :debian-slim/buster-slim
                                           11       :debian-slim/buster-slim
                                           :default :ubuntu/noble}
                  cfg/default-jdk-version 11
                  cfg/maintainers         ["Paul Lam <paul@quantisan.com>"
                                           "Wes Morgan <wesmorgan@icloud.com>"]
                  cfg/architectures       #{"amd64" "arm64v8"}]
      (let [variants (image-variants {8        ["debian"]
                                      11       ["debian"]
                                      :default ["eclipse-temurin"]}
                                     #{8 11 17 18}
                                     {"debian" #{:debian/buster
                                                 :debian-slim/buster-slim}
                                      :default #{:alpine/alpine :ubuntu/noble}}
                                     {"lein"       "2.9.1"
                                      "tools-deps" "1.10.1.478"}
                                     #{"amd64" "arm64v8"})]
        ;; filter is to make failure output a little more humane
        (are [v] (contains? (->> variants
                                 (filter #(and (= (:base-image %) (:base-image v))
                                               (= (:jdk-version %) (:jdk-version v))
                                               (= (:distro %) (:distro v))
                                               (= (:build-tool %) (:build-tool v))))
                                 set)
                            v)
                 {:jdk-version  11, :distro :debian-slim/buster-slim, :build-tool "lein"
                  :base-image   "debian" :base-image-tag "debian:buster-slim"
                  :maintainer   "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag   "temurin-11-lein-2.9.1", :build-tool-version "2.9.1"
                  :architecture "amd64"}
                 {:jdk-version  11, :distro :debian-slim/buster-slim, :build-tool "lein"
                  :base-image   "debian" :base-image-tag "debian:buster-slim"
                  :maintainer   "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag   "temurin-11-lein-2.9.1", :build-tool-version "2.9.1"
                  :architecture "arm64v8"}
                 {:jdk-version        18, :distro :ubuntu/noble
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:18-jdk-noble"
                  :build-tool         "tools-deps"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-18-tools-deps-1.10.1.478"
                  :build-tool-version "1.10.1.478"
                  :architecture       "amd64"}
                 {:jdk-version        18, :distro :ubuntu/noble
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:18-jdk-noble"
                  :build-tool         "tools-deps"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-18-tools-deps-1.10.1.478"
                  :build-tool-version "1.10.1.478"
                  :architecture       "arm64v8"}
                 {:jdk-version  11, :distro :debian/buster, :build-tool "lein"
                  :base-image   "debian" :base-image-tag "debian:buster"
                  :maintainer   "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag   "temurin-11-lein-2.9.1-buster", :build-tool-version "2.9.1"
                  :architecture "amd64"}
                 {:jdk-version  11, :distro :debian/buster, :build-tool "lein"
                  :base-image   "debian" :base-image-tag "debian:buster"
                  :maintainer   "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag   "temurin-11-lein-2.9.1-buster", :build-tool-version "2.9.1"
                  :architecture "arm64v8"}
                 {:jdk-version        11, :distro :debian/buster
                  :base-image         "debian"
                  :base-image-tag     "debian:buster"
                  :build-tool         "tools-deps"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-11-tools-deps-1.10.1.478-buster"
                  :build-tool-version "1.10.1.478"
                  :architecture       "amd64"}
                 {:jdk-version        11, :distro :debian/buster
                  :base-image         "debian"
                  :base-image-tag     "debian:buster"
                  :build-tool         "tools-deps"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-11-tools-deps-1.10.1.478-buster"
                  :build-tool-version "1.10.1.478"
                  :architecture       "arm64v8"}
                 {:jdk-version    8, :distro :debian-slim/buster-slim, :build-tool "lein"
                  :base-image     "debian"
                  :base-image-tag "debian:buster-slim"
                  :maintainer     "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag     "temurin-8-lein-2.9.1", :build-tool-version "2.9.1"
                  :architecture   "amd64"}
                 {:jdk-version    8, :distro :debian-slim/buster-slim, :build-tool "lein"
                  :base-image     "debian"
                  :base-image-tag "debian:buster-slim"
                  :maintainer     "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag     "temurin-8-lein-2.9.1", :build-tool-version "2.9.1"
                  :architecture   "arm64v8"}
                 {:jdk-version        8, :distro :debian-slim/buster-slim
                  :build-tool         "tools-deps"
                  :base-image         "debian"
                  :base-image-tag     "debian:buster-slim"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-8-tools-deps-1.10.1.478"
                  :build-tool-version "1.10.1.478"
                  :architecture       "amd64"}
                 {:jdk-version        8, :distro :debian-slim/buster-slim
                  :build-tool         "tools-deps"
                  :base-image         "debian"
                  :base-image-tag     "debian:buster-slim"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-8-tools-deps-1.10.1.478"
                  :build-tool-version "1.10.1.478"
                  :architecture       "arm64v8"}
                 {:jdk-version        17, :distro :ubuntu/noble, :build-tool "lein"
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:17-jdk-noble"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-17-lein-2.9.1"
                  :build-tool-version "2.9.1"
                  :architecture       "amd64"}
                 {:jdk-version        17, :distro :ubuntu/noble, :build-tool "lein"
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:17-jdk-noble"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-17-lein-2.9.1"
                  :build-tool-version "2.9.1"
                  :architecture       "arm64v8"}
                 {:jdk-version        17, :distro :alpine/alpine, :build-tool "lein"
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:17-jdk-alpine"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-17-lein-2.9.1-alpine"
                  :build-tool-version "2.9.1"
                  :architecture       "amd64"}
                 {:jdk-version        17, :distro :alpine/alpine, :build-tool "lein"
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:17-jdk-alpine"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-17-lein-2.9.1-alpine"
                  :build-tool-version "2.9.1"
                  :architecture       "arm64v8"}
                 {:jdk-version        17, :distro :ubuntu/noble
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:17-jdk-noble"
                  :build-tool         "tools-deps"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-17-tools-deps-1.10.1.478"
                  :build-tool-version "1.10.1.478"
                  :architecture       "amd64"}
                 {:jdk-version        17, :distro :ubuntu/noble
                  :base-image         "eclipse-temurin"
                  :base-image-tag     "eclipse-temurin:17-jdk-noble"
                  :build-tool         "tools-deps"
                  :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                  :docker-tag         "temurin-17-tools-deps-1.10.1.478"
                  :build-tool-version "1.10.1.478"
                  :architecture       "arm64v8"})))))

(deftest exclude?-test
  (testing "excludes variant that matches all key-values in any exclusion"
    (is (exclude? #{{:base-image "bad"}
                    {:base-image "not-great", :build-tool "woof"}}
                  {:base-image         "not-great" :build-tool "woof"
                   :build-tool-version "1.2.3"})))
  (testing "does not exclude partial matches"
    (is (not (exclude? #{{:base-image "bad", :build-tool "woof"}}
                       {:base-image "bad", :build-tool "lein"})))))
