(ns docker-clojure.variant-test
  (:require [clojure.test :refer :all]
            [docker-clojure.variant :refer :all]
            [docker-clojure.config :as cfg]))

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
                      ["build-tool" "1.2.3"] "i386")))))))
