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

;; The commit each release tag is expected to point at. After cloning we assert
;; HEAD matches, so a moved or re-pointed upstream tag can't slip a different
;; commit past us (belt-and-suspenders with `git verify-tag`). Requested by the
;; docker-library/official-images maintainers.
(def release-commits
  {"2.13.0" "d703e4802feb3e5c3fa9ae9f1874fb7a3a3e3030"
   "2.13.1" "d109a01c2bc295c1ed218ed335c36481938b745b"})

(defn release-commit [version]
  (or (get release-commits version)
      (throw (ex-info (str "No known Git commit for Leiningen " version
                           "; add it to lein/release-commits before building.")
                      {:lein-version version}))))

(defn install [_installer-hashes {:keys [build-tool-version] :as variant}]
  (render-template
   "templates/lein.tmpl"
   {:lein-version    build-tool-version
    :lein-commit     (release-commit build-tool-version)
    :clojure-version bundled-clojure-version
    :gpg-key         signing-key
    :install-deps    (install-deps variant)
    :uninstall-deps  (uninstall-build-deps variant)}))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    "CMD [\"repl\"]"
    "CMD [\"lein\", \"repl\"]"))
