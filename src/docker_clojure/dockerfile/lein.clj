(ns docker-clojure.dockerfile.lein
  (:require [clojure.string :as str]
            [docker-clojure.dockerfile.shared :refer :all]))

(def distro-deps
  {:debian-slim {:build   #{"wget" "gnupg"}
                 :runtime #{}}
   :debian      {:build   #{"gnupg"}
                 :runtime #{}}
   :alpine      {:build   #{"tar" "gnupg" "openssl" "ca-certificates"}
                 :runtime #{"bash"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

(defn install [{:keys [build-tool-version] :as variant}]
  (let [install-dep-cmds   (install-deps variant)
        uninstall-dep-cmds (uninstall-build-deps variant)]
    (-> [(format "ENV LEIN_VERSION=%s" build-tool-version)
         "ENV LEIN_INSTALL=/usr/local/bin/"
         ""
         "WORKDIR /tmp"
         ""
         "# Download the whole repo as an archive"
         "RUN \\"]
        (concat-commands install-dep-cmds)
        (concat-commands
          ["mkdir -p $LEIN_INSTALL"
           "wget -q https://raw.githubusercontent.com/technomancy/leiningen/$LEIN_VERSION/bin/lein-pkg"
           "echo \"Comparing lein-pkg checksum ...\""
           "sha256sum lein-pkg"
           "echo \"094b58e2b13b42156aaf7d443ed5f6665aee27529d9512f8d7282baa3cc01429 *lein-pkg\" | sha256sum -c -"
           "mv lein-pkg $LEIN_INSTALL/lein"
           "chmod 0755 $LEIN_INSTALL/lein"
           "wget -q https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip"
           "wget -q https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip.asc"
           "gpg --batch --keyserver keys.openpgp.org --recv-key 20242BACBBE95ADA22D0AFD7808A33D379C806C3"
           "echo \"Verifying file PGP signature...\""
           "gpg --batch --verify leiningen-$LEIN_VERSION-standalone.zip.asc leiningen-$LEIN_VERSION-standalone.zip"
           "rm leiningen-$LEIN_VERSION-standalone.zip.asc"
           "mkdir -p /usr/share/java"
           "mv leiningen-$LEIN_VERSION-standalone.zip /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar"]
          (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (concat
          [""
           "ENV PATH=$PATH:$LEIN_INSTALL"
           "ENV LEIN_ROOT 1"
           ""
           "# Install clojure 1.10.3 so users don't have to download it every time"
           "RUN echo '(defproject dummy \"\" :dependencies [[org.clojure/clojure \"1.10.3\"]])' > project.clj \\"
           "  && lein deps && rm project.clj"])

        (->> (remove nil?)))))

(def entrypoint
  ["ENTRYPOINT [\"lein\"]"])

(def command
  ["CMD [\"repl\"]"])

(defn contents [variant]
  (concat (install variant) [""] entrypoint command))
