(ns docker-clojure.dockerfile-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [docker-clojure.dockerfile :refer :all]
            [docker-clojure.core :as core]
            [docker-clojure.dockerfile.lein :as lein]
            [docker-clojure.dockerfile.boot :as boot]
            [docker-clojure.dockerfile.tools-deps :as tools-deps]))

(deftest build-dir-test
  (testing "replaces colons with hyphens in tag"
    (is (= "target/openjdk-11-alpine/lein"
           (build-dir {:base-image "openjdk:11-alpine"
                       :build-tool "lein"})))))

(deftest contents-test
  (testing "includes 'FROM base-image'"
    (is (str/includes? (contents core/installer-hashes {:base-image "base:foo"
                                                        :build-tool "boot"})
                       "FROM base:foo")))
  (testing "has no labels (Docker recommends against for base images)"
    (is (not (str/includes? (contents core/installer-hashes
                                      {:base-image "base:foo"
                                       :build-tool "boot"
                                       :maintainer "Me Myself"})
                            "LABEL "))))
  (testing "lein variant includes lein-specific contents"
    (with-redefs [lein/contents (constantly ["leiningen vs. the ants"])]
      (is (str/includes? (contents core/installer-hashes
                                   {:base-image "base:foo"
                                    :build-tool "lein"
                                    :maintainer "Me Myself"})
                         "leiningen vs. the ants"))))
  (testing "boot variant includes boot-specific contents"
    (with-redefs [boot/contents (constantly ["Booty McBootface"])]
      (is (str/includes? (contents core/installer-hashes
                                   {:base-image "base:foo"
                                    :build-tool "boot"
                                    :maintainer "Me Myself"})
                         "Booty McBootface"))))
  (testing "tools-deps variant includes tools-deps-specific contents"
    (with-redefs [tools-deps/contents (constantly
                                       ["Tools Deps is not a build tool"])]
      (is (str/includes? (contents core/installer-hashes
                                   {:base-image "base:foo"
                                    :build-tool "tools-deps"
                                    :maintainer "Me Myself"})
                         "Tools Deps is not a build tool")))))
