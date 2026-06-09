(ns docker-clojure.dockerfile.lein
  (:require [docker-clojure.dockerfile.shared
             :refer [install-distro-deps render-template
                     uninstall-distro-build-deps]]))

(defn prereqs [_ _] nil)

;; Leiningen no longer publishes a standalone uberjar, so we build it from
;; source: clone the GPG-signed release tag, bootstrap leiningen-core's deps
;; with Maven, and run `lein uberjar`. That needs git, gnupg & Maven at build
;; time.
(def distro-deps
  {:debian-slim {:build   #{"git" "gnupg" "maven"}
                 :runtime #{}}
   :debian      {:build   #{"git" "gnupg" "maven"}
                 :runtime #{"make"}}
   :ubuntu      {:build   #{"git" "gnupg" "maven"}
                 :runtime #{"make"}}
   :alpine      {:build   #{"git" "gnupg" "maven" "ca-certificates"}
                 :runtime #{"bash"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

;; Clojure version pre-installed into lein images so users don't download it on
;; first use.
(def ^:const bundled-clojure-version "1.12.5")

;; Leiningen release tags are signed with this key (Phil Hagelberg).
(def ^:const signing-key "9D13D9426A0814B3373CF5E3D8A8243577A7859F")

(defn install [_installer-hashes {:keys [build-tool-version] :as variant}]
  (render-template
   "templates/lein.tmpl"
   {:lein-version    build-tool-version
    :clojure-version bundled-clojure-version
    :gpg-key         signing-key
    :install-deps    (install-deps variant)
    :uninstall-deps  (uninstall-build-deps variant)}))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    "CMD [\"repl\"]"
    "CMD [\"lein\", \"repl\"]"))
