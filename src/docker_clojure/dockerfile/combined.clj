(ns docker-clojure.dockerfile.combined
  (:require
   [docker-clojure.dockerfile.lein :as lein]
   [docker-clojure.dockerfile.shared :as shared]
   [docker-clojure.dockerfile.tools-deps :as tools-deps]))

(def distro-deps
  {:debian-slim {:build   #{"curl" "gnupg"}
                 :runtime #{"rlwrap" "make" "git"}}
   :debian      {:build   #{"curl" "gnupg"}
                 :runtime #{"rlwrap" "make" "git"}}
   :ubuntu      {:build   #{"gnupg"}
                 ;; install curl as a runtime dep b/c we need it at build time
                 ;; but upstream includes it so we don't want to uninstall it
                 :runtime #{"rlwrap" "make" "git" "curl"}}
   :alpine      {:build   #{"curl" "tar" "gnupg" "openssl" "ca-certificates"}
                 :runtime #{"bash" "make" "git"}}})

(defn install [variant]
  (concat
   (lein/env-preamble)
   (tools-deps/env-preamble)
   [""
    "WORKDIR /tmp"
    ""]
   (shared/multiline-RUN
    (concat (shared/install-distro-deps distro-deps variant)
            (lein/installation-commands)
            (tools-deps/installation-commands)
            (shared/uninstall-distro-build-deps distro-deps variant)))
   [""]
   (lein/installation-postamble)
   (tools-deps/installation-postamble)))

(defn contents [{:keys [build-tool] :as variant}]
  (concat (install variant)
          [""]
          (shared/entrypoint variant)
          (case build-tool
            :lein (lein/command variant)
            :tools-deps (tools-deps/command variant))))

#_(contents {:build-tool :lein, :build-tool-version "2.11.2", :jdk-version 17})
