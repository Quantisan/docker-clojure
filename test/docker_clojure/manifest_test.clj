(ns docker-clojure.manifest-test
  (:require [clojure.test :refer [deftest is are testing]]
            [docker-clojure.manifest :refer [variant->manifest]]))

(deftest variant->manifest-test
  (testing "generates the correct manifest text for a variant"
    (is (= "\nTags: temurin-17-noble, temurin-17-tools-deps-1.10.1.478, temurin-17-tools-deps-1.10.1.478-noble, temurin-17-tools-deps-noble\nDirectory: target/eclipse-temurin-17-jdk-noble/tools-deps"
           (variant->manifest
            {:jdk-version 17, :distro :ubuntu/noble
             :base-image "eclipse-temurin"
             :base-image-tag "eclipse-temurin:17-jdk-noble"
             :build-tool "tools-deps"
             :maintainer "Paul Lam <p aul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com}>"
             :docker-tag "temurin-17-tools-deps-1.10.1.478"
             :build-tool-version "1.10.1.478"})))))
