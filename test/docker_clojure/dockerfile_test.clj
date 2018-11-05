(ns docker-clojure.dockerfile-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [docker-clojure.dockerfile :refer :all]
            [docker-clojure.dockerfile.lein :as lein]
            [docker-clojure.dockerfile.boot :as boot]
            [docker-clojure.dockerfile.tools-deps :as tools-deps]))

(deftest base-image-tag-test
  (testing "debian is omitted"
    (is (= "base" (base-image-tag {:base-image "base", :distro "debian"}))))
  (testing "other distros are added to base-image"
    (is (= "base-alpine" (base-image-tag {:base-image "base"
                                          :distro "alpine"})))))

(deftest base-image-filename-test
  (testing "replaces colons with hyphens in tag"
    (is (= "openjdk-11-alpine"
           (base-image-filename {:base-image "openjdk:11"
                                 :distro     "alpine"})))))

(deftest filename-test
  (testing "starts with 'Dockerfile'"
    (is (str/starts-with? (filename {:base-image "openjdk:11"})
                          "Dockerfile")))
  (with-redefs [base-image-filename (constantly "foo")]
    (testing "includes base-image-filename"
      (is (str/includes? (filename {}) "foo"))))
  (testing "includes build-tool"
    (is (str/includes? (filename {:build-tool "lein"}) "lein"))))

(deftest contents-test
  (testing "includes 'FROM base-image'"
    (is (str/includes? (contents {:base-image "base:foo"
                                  :build-tool "boot"})
                       "FROM base:foo")))
  (testing "includes maintainer label"
    (is (str/includes? (contents {:base-image "base:foo"
                                  :build-tool "boot"
                                  :maintainer "Me Myself"})
                       "LABEL maintainer=\"Me Myself\"")))
  (testing "lein variant includes lein-specific contents"
    (with-redefs [lein/contents (constantly ["leiningen vs. the ants"])]
      (is (str/includes? (contents {:base-image "base:foo"
                                    :build-tool "lein"
                                    :maintainer "Me Myself"})
                         "leiningen vs. the ants"))))
  (testing "boot variant includes boot-specific contents"
    (with-redefs [boot/contents (constantly ["Booty McBootface"])]
      (is (str/includes? (contents {:base-image "base:foo"
                                    :build-tool "boot"
                                    :maintainer "Me Myself"})
                         "Booty McBootface"))))
  (testing "tools-deps variant includes tools-deps-specific contents"
    (with-redefs [tools-deps/contents (constantly
                                       ["Tools Deps is not a build tool"])]
      (is (str/includes? (contents {:base-image "base:foo"
                                    :build-tool "tools-deps"
                                    :maintainer "Me Myself"})
                         "Tools Deps is not a build tool")))))
