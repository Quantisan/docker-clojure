(ns docker-clojure.dockerfile.lein
  (:require [docker-clojure.dockerfile.shared :refer :all]))

(defn prereqs [_ _] nil)

(def distro-deps
  {:debian-slim {:build   #{"wget" "gnupg"}
                 :runtime #{}}
   :debian      {:build   #{"gnupg"}
                 :runtime #{"make"}}
   :ubuntu      {:build   #{"wget" "gnupg"}
                 :runtime #{"make"}}
   :alpine      {:build   #{"tar" "gnupg" "openssl" "ca-certificates"}
                 :runtime #{"bash"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

(defn install [installer-hashes {:keys [build-tool-version] :as variant}]
  (let [install-dep-cmds   (install-deps variant)
        uninstall-dep-cmds (uninstall-build-deps variant)]
    (-> [(format "ENV LEIN_VERSION=%s" build-tool-version)
         "ENV LEIN_INSTALL=/usr/local/bin/"
         ""
         "WORKDIR /tmp"
         ""
         "# Download the whole repo as an archive"
         "RUN set -eux; \\"]
        (concat-commands install-dep-cmds)
        (concat-commands
          ["mkdir -p $LEIN_INSTALL"
           "wget -q https://codeberg.org/leiningen/leiningen/raw/tag/$LEIN_VERSION/bin/lein-pkg"
           "echo \"Comparing lein-pkg checksum ...\""
           "sha256sum lein-pkg"
           (str "echo \"" (get-in installer-hashes ["lein" build-tool-version]) " *lein-pkg\" | sha256sum -c -")
           "mv lein-pkg $LEIN_INSTALL/lein"
           "chmod 0755 $LEIN_INSTALL/lein"
           "export GNUPGHOME=\"$(mktemp -d)\""
           "export FILENAME_EXT=jar"
           "if printf '%s\\n%s\\n' \"2.9.7\" \"$LEIN_VERSION\" | sort -cV; then \\
              gpg --batch --keyserver hkps://keys.openpgp.org --recv-keys 6A2D483DB59437EBB97D09B1040193357D0606ED; \\
            else \\
              gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys 20242BACBBE95ADA22D0AFD7808A33D379C806C3; \\
              FILENAME_EXT=zip; \\
            fi"
           "wget -q https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT"
           "wget -q https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT.asc"
           "echo \"Verifying file PGP signature...\""
           "gpg --batch --verify leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT.asc leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT"
           "gpgconf --kill all"
           "rm -rf \"$GNUPGHOME\" leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT.asc"
           "mkdir -p /usr/share/java"
           "mv leiningen-$LEIN_VERSION-standalone.$FILENAME_EXT /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar"]
          (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (concat
          [""
           "ENV PATH=$PATH:$LEIN_INSTALL"
           "ENV LEIN_ROOT 1"
           ""
           "# Install clojure 1.11.1 so users don't have to download it every time"
           "RUN echo '(defproject dummy \"\" :dependencies [[org.clojure/clojure \"1.11.1\"]])' > project.clj \\"
           "  && lein deps && rm project.clj"])

        (->> (remove nil?)))))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    ["CMD [\"repl\"]"]
    ["CMD [\"lein\", \"repl\"]"]))

(defn contents [installer-hashes variant]
  (concat (install installer-hashes variant) [""] (entrypoint variant) (command variant)))
