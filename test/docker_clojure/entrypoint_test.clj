(ns docker-clojure.entrypoint-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [docker-clojure.dockerfile :as df])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- nul-fields [s]
  (vec (butlast (str/split s #"\x00" -1))))

(defn- run-entrypoint [build-tool & args]
  (let [dir (.toFile (Files/createTempDirectory "entrypoint test " (make-array FileAttribute 0)))
        bin (io/file dir "bin")
        log (io/file dir "calls")]
    (try
      (.mkdirs bin)
      (df/shared-prereqs (str bin) {:build-tool build-tool})
      ;; Record every tool invocation, including probes with hidden output.
      (doseq [tool ["lein" "clj"]]
        (let [file (io/file bin tool)]
          (spit file (str "#!/bin/sh\nprintf '%s\\0' '" tool "' \"$@\" >> \"$CALLS_LOG\"\n"))
          (.setExecutable file true false)))
      (spit (io/file dir "example.clj") "(println :example)")
      ;; A local script must not take precedence over the executable on PATH.
      (spit (io/file dir "echo") "(throw (Exception. \"unexpected project execution\"))")
      (spit (io/file dir "launch") "#!/bin/sh\nprintf '%s\\n' external\n")
      (.setExecutable (io/file dir "launch") true false)
      (let [result (apply sh (str (io/file bin "entrypoint"))
                          (concat args [:dir (str dir)
                                        :env {"PATH" (str bin ":/usr/bin:/bin")
                                              "CALLS_LOG" (str log)}]))]
        (assoc result :calls (if (.exists log) (nul-fields (slurp log)) [])))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file))))))

(deftest external-commands-do-not-probe-test
  (doseq [build-tool ["lein" "tools-deps" :docker-clojure.core/all]]
    (testing (str build-tool " runs executables without launching a build tool")
      (is (= {:exit 0 :out "hello\n" :err "" :calls []}
             (run-entrypoint build-tool "echo" "hello")))
      (is (= {:exit 0 :out "external\n" :err "" :calls []}
             (run-entrypoint build-tool "./launch")))
      (is (= {:exit 23 :out "" :err "failed\n" :calls []}
             (run-entrypoint build-tool "/bin/sh" "-c" "echo failed >&2; exit 23"))))))

(deftest defaults-and-flags-test
  (doseq [[build-tool tool] [["lein" "lein"] ["tools-deps" "clj"]
                             [:docker-clojure.core/all "clj"]]
          args [[] ["-h"] ["-M" "-e" "(+ 1 2)"]]]
    (let [result (apply run-entrypoint build-tool args)]
      (is (= 0 (:exit result)))
      (is (= (into [tool] args) (:calls result))))))

(deftest lein-shorthand-and-explicit-invocation-test
  (testing "built-in tasks and aliases keep precedence over executables"
    (doseq [task ["repl" "test" "cp" "install" "help" "static-classpath"
                  "uberjar" "überjar"]]
      (is (= ["lein" task] (:calls (run-entrypoint "lein" task))))))
  (testing "explicit commands and paths bypass shorthand routing"
    (is (= ["lein" "my-alias"] (:calls (run-entrypoint "lein" "lein" "my-alias"))))
    (is (= {:exit 0 :out "" :err "" :calls []}
           (run-entrypoint "lein" "/bin/test" "1" "=" "1"))))
  (testing "custom tasks use explicit Leiningen invocation"
    (is (= ["lein" "my-task"] (:calls (run-entrypoint "lein" "lein" "my-task"))))))

(deftest clojure-scripts-run-once-test
  (doseq [build-tool ["tools-deps" :docker-clojure.core/all]
          script ["example.clj" "./example.clj"]]
    (let [result (run-entrypoint build-tool script "argument")]
      (is (= 0 (:exit result)))
      (is (= ["clj" script "argument"] (:calls result))))))

(deftest unknown-commands-do-not-launch-project-test
  (doseq [build-tool ["lein" "tools-deps" :docker-clojure.core/all]]
    (let [result (run-entrypoint build-tool "no-such-command")]
      (is (= 127 (:exit result)))
      (is (str/includes? (:err result) "no-such-command"))
      (is (= [] (:calls result))))))

(deftest literal-arguments-test
  (let [args ["" "two words" "*.clj" "line one\nline two" "$(exit 99)" "'quoted'"]]
    (doseq [[build-tool command] [["lein" "test"] ["tools-deps" "-M"]]]
      (let [result (apply run-entrypoint build-tool command args)]
        (is (= 0 (:exit result)))
        (is (= (into [(if (= build-tool "lein") "lein" "clj") command] args)
               (:calls result)))))
    (doseq [build-tool ["lein" "tools-deps"]]
      (let [result (apply run-entrypoint build-tool "printf" "%s\\0" args)]
        (is (= 0 (:exit result)))
        (is (= args (nul-fields (:out result))))
        (is (= [] (:calls result)))))))
