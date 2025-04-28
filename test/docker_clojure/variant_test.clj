(ns docker-clojure.variant-test
  (:refer-clojure :exclude [compare sort])
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [docker-clojure.config :as cfg]
            [docker-clojure.core :as-alias core]
            [docker-clojure.variant :refer :all]))

(deftest ->map-test
  (testing "returns the expected map version of the image variant list"
    (with-redefs [cfg/maintainers ["Paul Lam <paul@quantisan.com>"
                                   "Wes Morgan <wesmorgan@icloud.com>"]]
      (is (= {:jdk-version        8
              :base-image         "eclipse-temurin"
              :base-image-tag     "eclipse-temurin:8-jdk-distro"
              :distro             :distro/distro
              :build-tool         "build-tool"
              :docker-tag         "temurin-8-build-tool-1.2.3-distro"
              :build-tool-version "1.2.3"
              :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
              :architecture       "i386"}
             (->map '("eclipse-temurin" 8 :distro/distro
                      ["build-tool" "1.2.3"] "i386"))))

      (is (= {:jdk-version        22
              :base-image         "debian"
              :base-image-tag     "debian:bookworm"
              :distro             :debian/bookworm
              :build-tool         "tools-deps"
              :docker-tag         "temurin-22-tools-deps-1.12.0.1530"
              :build-tool-version "1.12.0.1530"
              :maintainer         "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
              :architecture       "arm64"}
             (->map '("debian" 22 :debian/bookworm
                      ["tools-deps" "1.12.0.1530"] "arm64"))))

      (is (= {:jdk-version         22
              :base-image          "debian"
              :base-image-tag      "debian:bookworm"
              :distro              :debian/bookworm
              :build-tool          ::core/all
              :docker-tag          "latest"
              :build-tool-version  nil
              :build-tool-versions cfg/build-tools
              :maintainer          "Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"
              :architecture        "arm64"}
             (->map '("debian" 22 :debian/bookworm
                      [::core/all] "arm64")))))))

(deftest exclude?-test
  (testing "gets excluded if contains every key-value pair in exclusion"
    (is (exclude? {:foo "bar" :baz "qux" :other "thingy"}
                  {:foo "bar" :baz "qux"})))
  (testing "gets excluded if pred fn returns true"
    (is (exclude? {:foo "bar" :baz "qux" :other "thingy"}
                  {:foo #(str/starts-with? % "b") :baz "qux"})))
  (testing "remains included if missing one key-value pair from exclusion"
    (is (not (exclude? {:foo "bar" :other "thingy"}
                       {:foo "bar" :baz "qux"}))))
  (testing "remains included if key's value doesn't match exclusion's"
    (is (not (exclude? {:foo "burp" :baz "qux" :other "thingy"}
                       {:foo "bar" :baz "qux"}))))
  (testing "remains included if pred fn returns false"
    (is (not (exclude? {:foo "bar" :baz "qux" :other "thingy"}
                       {:foo "bar" :baz #(> (count %) 3)})))))
