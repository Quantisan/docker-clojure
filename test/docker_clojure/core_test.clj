(ns docker-clojure.core-test
  (:require [clojure.test :refer :all]
            [docker-clojure.core :refer :all]
            [clojure.string :as str]))

(deftest default-distro-test
  (testing "jdk-version 8 gets slim-buster"
    (is (= "slim-buster" (default-distro 8))))
  (testing "jdk-version 11 gets slim-buster"
    (is (= "slim-buster" (default-distro 11))))
  (testing "other versions get slim-buster"
    (is (= "slim-buster" (default-distro 12)))
    (is (= "slim-buster" (default-distro 14)))
    (is (= "slim-buster" (default-distro 15)))))

(deftest image-variants-test
  (testing "generates the expected set of variants"
    (let [variants (image-variants #{8 11 14 15}
                                   #{"buster" "slim-buster" "alpine"}
                                   {"lein"       "2.9.1"
                                    "boot"       "2.8.3"
                                    "tools-deps" "1.10.1.478"})]
      ;; filter is to make failure output a little more humane
      (are [v] (contains? (->> variants
                               (filter #(and (= (:jdk-version %) (:jdk-version v))
                                             (= (:distro %)      (:distro v))
                                             (= (:build-tool %)  (:build-tool v))))
                               set)
                          v)
            {:jdk-version 11, :distro "slim-buster", :build-tool "lein"
                 :base-image "openjdk:11-slim-buster"
                 :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                 :docker-tag "lein-2.9.1", :build-tool-version "2.9.1"}
            {:jdk-version 11, :distro "slim-buster", :build-tool "boot"
                :base-image "openjdk:11-slim-buster"
                :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                :docker-tag "boot-2.8.3", :build-tool-version "2.8.3"}
            {:jdk-version 11, :distro "slim-buster"
                :base-image "openjdk:11-slim-buster"
                :build-tool "tools-deps"
                :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                :docker-tag "tools-deps-1.10.1.478"
                :build-tool-version "1.10.1.478"}
            {:jdk-version 11, :distro "buster", :build-tool "lein"
                :base-image "openjdk:11-buster"
                :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                :docker-tag "lein-2.9.1-buster", :build-tool-version "2.9.1"}
            {:jdk-version 11, :distro "buster", :build-tool "boot"
                :base-image "openjdk:11-buster"
                :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
                :docker-tag "boot-2.8.3-buster", :build-tool-version "2.8.3"}
            {:jdk-version 11, :distro "buster"
             :base-image "openjdk:11-buster"
             :build-tool "tools-deps"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "tools-deps-1.10.1.478-buster"
             :build-tool-version "1.10.1.478"}
            {:jdk-version 8, :distro "slim-buster", :build-tool "lein"
             :base-image "openjdk:8-slim-buster"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "openjdk-8-lein-2.9.1", :build-tool-version "2.9.1"}
            {:jdk-version 8, :distro "slim-buster", :build-tool "boot"
             :base-image "openjdk:8-slim-buster"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "openjdk-8-boot-2.8.3", :build-tool-version "2.8.3"}
            {:jdk-version 8, :distro "slim-buster"
             :build-tool "tools-deps"
             :base-image "openjdk:8-slim-buster"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "openjdk-8-tools-deps-1.10.1.478"
             :build-tool-version "1.10.1.478"}
            {:jdk-version 14, :distro "slim-buster", :build-tool "lein"
             :base-image "openjdk:14-slim-buster"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "openjdk-14-lein-2.9.1"
             :build-tool-version "2.9.1"}
            {:jdk-version 15, :distro "alpine", :build-tool "lein"
             :base-image "openjdk:15-alpine"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "openjdk-15-lein-2.9.1-alpine"
             :build-tool-version "2.9.1"}
            {:jdk-version 15, :distro "alpine", :build-tool "boot"
             :base-image "openjdk:15-alpine"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "openjdk-15-boot-2.8.3-alpine"
             :build-tool-version "2.8.3"}
            {:jdk-version 15, :distro "alpine"
             :base-image "openjdk:15-alpine"
             :build-tool "tools-deps"
             :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
             :docker-tag "openjdk-15-tools-deps-1.10.1.478-alpine"
             :build-tool-version "1.10.1.478"}))))

(deftest variant-map-test
  (testing "returns the expected map version of the image variant list"
    (is (= {:jdk-version        8
            :base-image         "openjdk:8-distro"
            :distro             "distro"
            :build-tool         "build-tool"
            :docker-tag         "openjdk-8-build-tool-1.2.3-distro"
            :build-tool-version "1.2.3"
            :maintainer "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"}
           (variant-map '(8 "distro" ["build-tool" "1.2.3"]))))))

(deftest exclude?-test
  (testing "excludes variant that matches all key-values in any exclusion"
    (is (exclude? #{{:base-image "bad"}
                    {:base-image "not-great", :build-tool "woof"}}
                  {:base-image "not-great" :build-tool "woof"
                   :build-tool-version "1.2.3"})))
  (testing "does not exclude partial matches"
    (is (not (exclude? #{{:base-image "bad", :build-tool "woof"}}
                       {:base-image "bad", :build-tool "boot"})))))

(deftest docker-tag-test
  (testing "default java version is left out"
    (is (not (str/includes? (docker-tag {:jdk-version 11})
                            "openjdk-11"))))
  (testing "non-default version is added as a prefix"
    (is (str/starts-with? (docker-tag {:jdk-version 14})
                          "openjdk-14")))
  (testing "default distro is left out"
    (is (not (str/includes? (docker-tag {:jdk-version 14
                                         :distro "slim-buster"})
                            "slim-buster"))))
  (testing "alpine is added as a suffix"
    (is (str/ends-with? (docker-tag {:jdk-version 8
                                     :distro "alpine"})
                        "alpine")))
  (testing "build tool is included"
    (is (str/includes? (docker-tag {:jdk-version 11
                                    :build-tool "lein"})
                       "lein")))
  (testing "build tool version is included"
    (is (str/includes? (docker-tag {:jdk-version 11
                                    :build-tool "boot"
                                    :build-tool-version "2.8.1"})
                       "2.8.1"))))
