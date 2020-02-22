(ns docker-clojure.dockerfile.lein
  (:require [clojure.string :as str]
            [docker-clojure.dockerfile.shared :refer :all]))

(def distro-deps
  {"slim-buster" {:build   #{"wget"}
                  :runtime #{}}
   "alpine"      {:build   #{"tar" "openssl"}
                  :runtime #{"bash"}}})

(defn install-deps [{:keys [distro]}]
  (case distro
    "slim-buster"
    ["apt-get update"
     (str/join " " (concat ["apt-get install -y"]
                           (all-deps distro-deps distro)))
     "rm -rf /var/lib/apt/lists/*"]

    "alpine"
    [(str/join " " (concat ["apk add --update --no-cache"]
                           (all-deps distro-deps distro)))]

    nil))

(defn uninstall-build-deps [{:keys [distro]}]
  (case distro
    "slim-buster"
    [(str/join " " (concat ["apt-get remove -y --purge"]
                           (build-deps distro-deps distro)))]

    "alpine"
    [(str/join " " (concat ["apk del"]
                           (build-deps distro-deps distro)))]

    nil))

(defn install [{:keys [build-tool-version] :as variant}]
  (let [install-dep-cmds (install-deps variant)
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
          "sha1sum lein-pkg"
          "echo \"93be2c23ab4ff2fc4fcf531d7510ca4069b8d24a *lein-pkg\" | sha1sum -c -"
          "mv lein-pkg $LEIN_INSTALL/lein"
          "chmod 0755 $LEIN_INSTALL/lein"
          "wget -q https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip"
          "mkdir -p /usr/share/java"
          "mv leiningen-$LEIN_VERSION-standalone.zip /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar"]
         (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (concat
         [""
          "ENV PATH=$PATH:$LEIN_INSTALL"
          "ENV LEIN_ROOT 1"
          ""
          "# Install clojure 1.10.1 so users don't have to download it every time"
          "RUN echo '(defproject dummy \"\" :dependencies [[org.clojure/clojure \"1.10.1\"]])' > project.clj \\"
          "  && lein deps && rm project.clj"])

        (->> (remove nil?)))))

(def command
  ["CMD [\"lein\", \"repl\"]"])

(defn contents [variant]
  (concat (install variant) [""] command))