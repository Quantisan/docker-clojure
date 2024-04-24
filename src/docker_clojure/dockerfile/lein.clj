(ns docker-clojure.dockerfile.lein
  (:require
   [clojure.string :as str]
   [docker-clojure.config :as cfg]))

(def ^:const gpg-key "9D13D9426A0814B3373CF5E3D8A8243577A7859F")

(defn env-preamble []
  [(format "ENV LEIN_VERSION=%s" (-> cfg/*build-tools* :lein :version))
   "ENV LEIN_INSTALL=/usr/local/bin/"
   "ENV LEIN_ROOT=1"
   "ENV PATH=$PATH:$LEIN_INSTALL"])

(defn installation-commands []
  ["echo Installing Leiningen"
   "mkdir -p $LEIN_INSTALL"
   "curl -fsSLO https://codeberg.org/leiningen/leiningen/raw/tag/$LEIN_VERSION/bin/lein-pkg"
   "echo \"Comparing lein-pkg checksum ...\""
   "sha256sum lein-pkg"
   (format "echo \"%s *lein-pkg\" | sha256sum -c -"
           (-> cfg/*build-tools* :lein :installer-hash))
   "mv lein-pkg $LEIN_INSTALL/lein"
   "chmod 0755 $LEIN_INSTALL/lein"
   "export GNUPGHOME=\"$(mktemp -d)\""
   (format "gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys %s"
           gpg-key)
   "curl -fsSLO https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.jar"
   "curl -fsSLO https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.jar.asc"
   "echo \"Verifying Leiningen file PGP signature...\""
   "gpg --batch --verify leiningen-$LEIN_VERSION-standalone.jar.asc leiningen-$LEIN_VERSION-standalone.jar"
   "gpgconf --kill all"
   "rm -rf \"$GNUPGHOME\" leiningen-$LEIN_VERSION-standalone.jar.asc"
   "mkdir -p /usr/share/java"
   "mv leiningen-$LEIN_VERSION-standalone.jar /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar"
   "mkdir -p ~/.lein/"])

(defn installation-postamble []
  [])

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    ["CMD [\"repl\"]"]
    ["CMD [\"lein\", \"repl\"]"]))
