(ns docker-clojure.manifest-test
  (:require [clojure.test :refer :all]
            [docker-clojure.manifest :refer :all]))

(deftest variant-tags-test
  (testing "Generates all-defaults tag for a build tool"
    (let [tags (variant-tags {:base-image         "eclipse-temurin"
                              :jdk-version        17
                              :distro             :ubuntu/jammy
                              :build-tool         "tools-deps"
                              :build-tool-version "1.11.1.1155"})]
      (is ((set tags) "tools-deps"))))
  (testing "Generates jdk-version-build-tool tag for every jdk version"
    (are [jdk-version tag]
      (let [tags (variant-tags {:base-image         "eclipse-temurin"
                                :jdk-version        jdk-version
                                :distro             :ubuntu/jammy
                                :build-tool         "tools-deps"
                                :build-tool-version "1.11.1.1155"})]
        ((set tags) tag))
      11 "temurin-11-tools-deps"
      17 "temurin-17-tools-deps"
      18 "temurin-18-tools-deps"))
  (testing "Generates build-tool-distro tag for every distro"
    (are [distro tag]
      (let [tags (variant-tags {:base-image         "eclipse-temurin"
                                :jdk-version        17
                                :distro             distro
                                :build-tool         "tools-deps"
                                :build-tool-version "1.11.1.1155"})]
        ((set tags) tag))
      :ubuntu/focal "tools-deps-focal"
      :ubuntu/jammy "tools-deps-jammy")))
