(ns docker-clojure.changes-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [docker-clojure.changes :refer [parse-diff select-build-dirs]]
            [docker-clojure.config :as cfg]))

(defn- modified-diff [path old new]
  (str/join "\n" [(str "diff --git a/" path " b/" path)
                  "index 1111111..2222222 100644"
                  (str "--- a/" path)
                  (str "+++ b/" path)
                  "@@ -3 +3 @@"
                  (str "-" old)
                  (str "+" new)]))

(defn- added-diff [path line]
  (str/join "\n" [(str "diff --git a/" path " b/" path)
                  "new file mode 100644"
                  "index 0000000..2222222"
                  "--- /dev/null"
                  (str "+++ b/" path)
                  "@@ -0,0 +1 @@"
                  (str "+" line)]))

(defn- deleted-diff [path line]
  (str/join "\n" [(str "diff --git a/" path " b/" path)
                  "deleted file mode 100644"
                  "index 1111111..0000000"
                  (str "--- a/" path)
                  "+++ /dev/null"
                  "@@ -1 +0,0 @@"
                  (str "-" line)]))

(deftest parse-diff-test
  (testing "an empty diff has no changes"
    (is (= {} (parse-diff ""))))
  (testing "build dirs with the same change share a signature"
    (let [changes (parse-diff
                   (str/join "\n"
                             [(modified-diff "target/a-25/lein/Dockerfile"
                                             "ENV V=1" "ENV V=2")
                              (modified-diff "target/b-21/lein/Dockerfile"
                                             "ENV V=1" "ENV V=2")
                              (modified-diff "target/b-21/tools-deps/Dockerfile"
                                             "ENV W=1" "ENV W=2")]))]
      (is (= #{"target/a-25/lein" "target/b-21/lein" "target/b-21/tools-deps"}
             (set (keys changes))))
      (is (not-any? :added? (vals changes)))
      (is (= (get-in changes ["target/a-25/lein" :signature])
             (get-in changes ["target/b-21/lein" :signature])))
      (is (not= (get-in changes ["target/a-25/lein" :signature])
                (get-in changes ["target/b-21/tools-deps" :signature])))))
  (testing "new build dirs are flagged as added"
    (let [changes (parse-diff
                   (str/join "\n"
                             [(added-diff "target/a-27/lein/Dockerfile"
                                          "FROM a:27")
                              (added-diff "target/a-27/lein/entrypoint"
                                          "#!/usr/bin/env bash")]))]
      (is (= ["target/a-27/lein"] (keys changes)))
      (is (:added? (get changes "target/a-27/lein")))))
  (testing "a new file in an existing build dir doesn't make it added"
    (let [changes (parse-diff
                   (str/join "\n"
                             [(modified-diff "target/a-25/lein/Dockerfile"
                                             "ENV V=1" "ENV V=2")
                              (added-diff "target/a-25/lein/entrypoint"
                                          "#!/usr/bin/env bash")]))]
      (is (false? (:added? (get changes "target/a-25/lein"))))))
  (testing "deleted build dirs and files outside build dirs are left out"
    (is (= {} (parse-diff
               (str/join "\n"
                         [(deleted-diff "target/a-8/lein/Dockerfile" "FROM a:8")
                          (deleted-diff "target/a-8/lein/entrypoint" "x")
                          (modified-diff "src/docker_clojure/config.clj"
                                         "a" "b")
                          (added-diff "target/.gitkeep" "")]))))))

(deftest select-build-dirs-test
  (with-redefs [cfg/default-jdk-version 25
                cfg/default-distros     {:default :debian/bookworm}
                cfg/default-build-tool  "tools-deps"]
    (let [variant  (fn [base-image-tag jdk-version distro build-tool]
                     {:base-image-tag base-image-tag
                      :jdk-version    jdk-version
                      :distro         distro
                      :build-tool     build-tool})
          variants [(variant "debian:bookworm" 21 :debian/bookworm "lein")
                    (variant "debian:bookworm" 25 :debian/bookworm "lein")
                    (variant "debian:bookworm" 25 :debian/bookworm "tools-deps")
                    (variant "debian:trixie" 25 :debian/trixie "lein")]
          same     {:added? false :signature #{:same}}]
      (testing "identical changes are represented by the default JDK & distro"
        (is (= ["target/debian-bookworm-25/lein"]
               (select-build-dirs variants
                                  {"target/debian-bookworm-21/lein" same
                                   "target/debian-bookworm-25/lein" same
                                   "target/debian-trixie-25/lein"   same}))))
      (testing "the default JDK wins when its default distro didn't change"
        (is (= ["target/debian-trixie-25/lein"]
               (select-build-dirs variants
                                  {"target/debian-bookworm-21/lein" same
                                   "target/debian-trixie-25/lein"   same}))))
      (testing "the default build tool wins when the JDK & distro tie"
        (is (= ["target/debian-bookworm-25/tools-deps"]
               (select-build-dirs variants
                                  {"target/debian-bookworm-25/lein"       same
                                   "target/debian-bookworm-25/tools-deps" same}))))
      (testing "each distinct change gets its own build"
        (is (= ["target/debian-bookworm-21/lein"
                "target/debian-bookworm-25/tools-deps"]
               (select-build-dirs
                variants
                {"target/debian-bookworm-21/lein"
                 {:added? false :signature #{:one}}
                 "target/debian-bookworm-25/tools-deps"
                 {:added? false :signature #{:other}}}))))
      (testing "a new distro is built for each JDK that gets it"
        (let [added    {:added? true :signature #{:a}}
              resolute (for [jdk-version [26 27]
                             build-tool  ["lein" "tools-deps"]]
                         (variant (str "temurin:" jdk-version "-jdk-resolute")
                                  jdk-version :ubuntu/resolute build-tool))]
          (is (= ["target/temurin-26-jdk-resolute/tools-deps"
                  "target/temurin-27-jdk-resolute/lein"]
                 (select-build-dirs
                  resolute
                  (zipmap ["target/temurin-26-jdk-resolute/lein"
                           "target/temurin-26-jdk-resolute/tools-deps"
                           "target/temurin-27-jdk-resolute/lein"
                           "target/temurin-27-jdk-resolute/tools-deps"]
                          (repeat added)))))))
      (testing "new build dirs cover each JDK, distro & build tool once"
        (let [added   {:added? true :signature #{:a}}
              jdk-27  (for [[tag distro] {"debian:bookworm" :debian/bookworm
                                          "debian:trixie"   :debian/trixie
                                          "alpine:27"       :alpine/alpine}
                            build-tool   ["lein" "tools-deps"]]
                        (variant tag 27 distro build-tool))
              changes (assoc (zipmap ["target/debian-bookworm-27/lein"
                                      "target/debian-bookworm-27/tools-deps"
                                      "target/debian-trixie-27/lein"
                                      "target/debian-trixie-27/tools-deps"
                                      "target/alpine-27/lein"
                                      "target/alpine-27/tools-deps"]
                                     (repeat added))
                             "target/debian-bookworm-21/lein" same
                             "target/debian-bookworm-25/lein" same)]
          (is (= ["target/alpine-27/lein"
                  "target/debian-bookworm-25/lein"
                  "target/debian-bookworm-27/tools-deps"
                  "target/debian-trixie-27/tools-deps"]
                 (select-build-dirs (concat variants jdk-27) changes)))))
      (testing "build dirs without a variant are ignored"
        (is (= [] (select-build-dirs variants
                                     {"target/nope-25/lein" same})))))))
