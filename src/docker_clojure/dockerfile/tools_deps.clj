(ns docker-clojure.dockerfile.tools-deps
  (:require
   [docker-clojure.config :as cfg]))

(defn env-preamble []
  [(format "ENV CLOJURE_CLI_VERSION=%s" (-> cfg/*build-tools* :tools-deps :version))])

(defn installation-commands []
  ["echo Installing tools.deps"
   "curl -fsSLO https://download.clojure.org/install/linux-install-$CLOJURE_CLI_VERSION.sh"
   "sha256sum linux-install-$CLOJURE_CLI_VERSION.sh"
   (format "echo \"%s *linux-install-$CLOJURE_CLI_VERSION.sh\" | sha256sum -c -"
           (-> cfg/*build-tools* :tools-deps :installer-hash))
   "chmod +x linux-install-$CLOJURE_CLI_VERSION.sh"
   "./linux-install-$CLOJURE_CLI_VERSION.sh"
   "rm linux-install-$CLOJURE_CLI_VERSION.sh"
   "clojure -e \"(clojure-version)\""])

(defn installation-postamble []
  ["# Docker bug makes rlwrap crash w/o short sleep first"
   "# Bug: https://github.com/moby/moby/issues/28009"
   "# As of 2021-09-10 this bug still exists, despite that issue being closed"
   "COPY rlwrap.retry /usr/local/bin/rlwrap"])

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    ["CMD [\"-M\", \"--repl\"]"]
    [(str "CMD [\"clj\"]")]))
