(ns docker-clojure.manifest-test
  (:require [clojure.test :refer :all]
            [docker-clojure.manifest :refer :all]))

(deftest variant-tags-test
  (testing "Generates all-defaults tag for a build tool"
    (let [tags (variant-tags {:base-image         "debian"
                              :jdk-version        21
                              :distro             :debian/bookworm
                              :build-tool         "tools-deps"
                              :build-tool-version "1.11.1.1155"})]
      (is ((set tags) "tools-deps"))))
  (testing "Generates jdk-version-build-tool tag for every jdk version"
    (are [jdk-version tag]
      (let [tags (variant-tags {:base-image         (if (< jdk-version 21)
                                                      "eclipse-temurin"
                                                      "debian")
                                :jdk-version        jdk-version
                                :distro             (if (< jdk-version 21)
                                                      :ubuntu/jammy
                                                      :debian/bookworm)
                                :build-tool         "tools-deps"
                                :build-tool-version "1.11.1.1155"})]
        ((set tags) tag))
      11 "temurin-11-tools-deps"
      17 "temurin-17-tools-deps"
      21 "temurin-21-tools-deps"))
  (testing "Generates build-tool-distro tag for every distro"
    (are [distro tag]
      (let [tags (variant-tags {:base-image         "debian"
                                :jdk-version        21
                                :distro             distro
                                :build-tool         "tools-deps"
                                :build-tool-version "1.11.1.1155"})]
        ((set tags) tag))
      :debian/bullseye "tools-deps-bullseye"
      :debian-slim/bullseye-slim "tools-deps-bullseye-slim"
      :debian/bookworm "tools-deps-bookworm"
      :debian-slim/bookworm-slim "tools-deps-bookworm-slim")))
