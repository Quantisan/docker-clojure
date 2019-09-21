(ns docker-clojure.core-test
  (:require [clojure.test :refer :all]
            [docker-clojure.core :refer :all]
            [clojure.string :as str]))

(deftest image-variants-test
  (testing "generates the expected set of variants"
    (is (= #{{:base-image "openjdk:8", :distro "debian", :build-tool "lein"
              :maintainer "Paul Lam <paul@quantisan.com>"
              :docker-tag "lein-2.9.1", :build-tool-version "2.9.1"}
             {:base-image "openjdk:8", :distro "debian", :build-tool "boot"
              :maintainer "Wes Morgan <wesmorgan@icloud.com>"
              :docker-tag "boot-2.8.3", :build-tool-version "2.8.3"}
             {:base-image "openjdk:8", :distro "debian"
              :build-tool "tools-deps"
              :maintainer "Kirill Chernyshov <delaguardo@gmail.com>"
              :docker-tag "tools-deps-1.10.1.469"
              :build-tool-version "1.10.1.469"}
             {:base-image "openjdk:8", :distro "alpine", :build-tool "lein"
              :maintainer "Wes Morgan <wesmorgan@icloud.com>"
              :docker-tag "lein-2.9.1-alpine", :build-tool-version "2.9.1"}
             {:base-image "openjdk:8", :distro "alpine", :build-tool "boot"
              :maintainer "Wes Morgan <wesmorgan@icloud.com>"
              :docker-tag "boot-2.8.3-alpine", :build-tool-version "2.8.3"}
             {:base-image "openjdk:8", :distro "alpine"
              :build-tool "tools-deps"
              :maintainer "Kirill Chernyshov <delaguardo@gmail.com>"
              :docker-tag "tools-deps-1.10.1.469-alpine"
              :build-tool-version "1.10.1.469"}
             {:base-image "openjdk:11", :distro "debian", :build-tool "lein"
              :maintainer "Paul Lam <paul@quantisan.com>"
              :docker-tag "openjdk-11-lein-2.9.1", :build-tool-version "2.9.1"}
             {:base-image "openjdk:11", :distro "debian", :build-tool "boot"
              :maintainer "Wes Morgan <wesmorgan@icloud.com>"
              :docker-tag "openjdk-11-boot-2.8.3", :build-tool-version "2.8.3"}
             {:base-image "openjdk:11", :distro "debian"
              :build-tool "tools-deps"
              :maintainer "Kirill Chernyshov <delaguardo@gmail.com>"
              :docker-tag "openjdk-11-tools-deps-1.10.1.469"
              :build-tool-version "1.10.1.469"}
             {:base-image "openjdk:11", :distro "alpine", :build-tool "lein"
              :maintainer "Wes Morgan <wesmorgan@icloud.com>"
              :docker-tag "openjdk-11-lein-2.9.1-alpine"
              :build-tool-version "2.9.1"}
             {:base-image "openjdk:11", :distro "alpine", :build-tool "boot"
              :maintainer "Wes Morgan <wesmorgan@icloud.com>"
              :docker-tag "openjdk-11-boot-2.8.3-alpine"
              :build-tool-version "2.8.3"}
             {:base-image "openjdk:11", :distro "alpine"
              :build-tool "tools-deps"
              :maintainer "Kirill Chernyshov <delaguardo@gmail.com>"
              :docker-tag "openjdk-11-tools-deps-1.10.1.469-alpine"
              :build-tool-version "1.10.1.469"}}
           (image-variants #{"openjdk:8" "openjdk:11"}
                           #{"debian" "alpine"}
                           #{"lein" "boot" "tools-deps"})))))

(deftest variant-map-test
  (testing "returns the expected map version of the image variant list"
    (with-redefs [maintainer (constantly "me")
                  build-tools {"third" "1.2.3"}]
      (is (= {:base-image         "first"
              :distro             "second"
              :build-tool         "third"
              :maintainer         "me"
              :docker-tag         "first-third-1.2.3-second"
              :build-tool-version "1.2.3"}
             (variant-map '("first" "second" "third")))))))

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
  (testing "openjdk:8 is left out"
    (is (not (str/includes? (docker-tag {:base-image "openjdk:8"})
                            "openjdk"))))
  (testing "openjdk:11 is added as a prefix"
    (is (str/starts-with? (docker-tag {:base-image "openjdk:11"})
                          "openjdk-11")))
  (testing "debian is left out"
    (is (not (str/includes? (docker-tag {:base-image "openjdk:8"
                                         :distro "debian"})
                            "debian"))))
  (testing "alpine is added as a suffix"
    (is (str/ends-with? (docker-tag {:base-image "openjdk:8"
                                     :distro "alpine"})
                        "alpine")))
  (testing "build tool is included"
    (is (str/includes? (docker-tag {:base-image "openjdk:8"
                                    :build-tool "lein"})
                       "lein")))
  (testing "build tool version is included"
    (is (str/includes? (docker-tag {:base-image "openjdk:8"
                                    :build-tool "boot"
                                    :build-tool-version "2.8.1"})
                       "2.8.1"))))

(deftest base-image->tag-component-test
  (testing "replaces colons with hyphens"
    (is (= "no-more-colons"
           (base-image->tag-component "no:more:colons")))))
