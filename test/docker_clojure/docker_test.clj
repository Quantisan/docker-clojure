(ns docker-clojure.docker-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [docker-clojure.config :as cfg]
            [docker-clojure.docker :refer :all]))

(deftest tag-test
  (with-redefs [cfg/default-jdk-version 11 ; TODO: Make this an arg to the fn instead
                cfg/default-distros     {:default :debian-slim/slim-buster}] ; TODO: Rethink this too?
    (testing "default java version is left out"
      (is (not (str/includes? (default-tag {:jdk-version 11})
                              "openjdk-11"))))
    (testing "non-default version is added as a prefix"
      (is (str/starts-with? (default-tag {:base-image  "openjdk"
                                          :jdk-version 14})
                            "openjdk-14")))
    (testing "default distro is left out"
      (is (not (str/includes? (default-tag {:jdk-version 14
                                            :distro      :debian-slim/slim-buster})
                              "slim-buster"))))
    (testing "alpine is added as a suffix"
      (is (str/ends-with? (default-tag {:jdk-version 8
                                        :distro      :alpine/alpine})
                          "alpine")))
    (testing "build tool is included"
      (is (str/includes? (default-tag {:jdk-version 11
                                       :build-tool  "lein"})
                         "lein")))
    (testing "build tool version is included"
      (is (str/includes? (default-tag {:jdk-version        11
                                       :build-tool         "lein"
                                       :build-tool-version "2.11.2"})
                         "2.11.2")))))

(deftest all-tags-test
  (testing "Generates all-defaults tag for a build tool"
    (let [tags (all-tags {:base-image         "debian"
                          :jdk-version        21
                          :distro             :debian/bookworm
                          :build-tool         "tools-deps"
                          :build-tool-version "1.11.1.1155"})]
      (is ((set tags) "tools-deps"))))
  (testing "Generates jdk-version-build-tool tag for every jdk version"
    (are [jdk-version tag]
      (let [tags (all-tags {:base-image         (if (< jdk-version 21)
                                                  "eclipse-temurin"
                                                  "debian")
                            :jdk-version        jdk-version
                            :distro             (if (< jdk-version 21)
                                                  :ubuntu/noble
                                                  :debian/bookworm)
                            :build-tool         "tools-deps"
                            :build-tool-version "1.11.1.1155"})]
        ((set tags) tag))
      11 "temurin-11-tools-deps"
      17 "temurin-17-tools-deps"
      21 "temurin-21-tools-deps"))
  (testing "Generates build-tool-distro tag for every distro"
    (are [distro tag]
      (let [tags (all-tags {:base-image         "debian"
                            :jdk-version        21
                            :distro             distro
                            :build-tool         "tools-deps"
                            :build-tool-version "1.11.1.1155"})]
        ((set tags) tag))
      :debian/bullseye "tools-deps-bullseye"
      :debian-slim/bullseye-slim "tools-deps-bullseye-slim"
      :debian/bookworm "tools-deps-bookworm"
      :debian-slim/bookworm-slim "tools-deps-bookworm-slim")))
