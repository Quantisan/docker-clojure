(ns docker-clojure.dockerfile.boot
  (:require [docker-clojure.dockerfile.shared :refer :all]
            [clojure.string :as str]))

(def distro-deps
  {:debian-slim {:build   #{"wget"}
                 :runtime #{}}
   :alpine      {:build   #{"openssl"}
                 :runtime #{"bash"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

(defn install [{:keys [build-tool-version] :as variant}]
  (let [install-dep-cmds   (install-deps variant)
        uninstall-dep-cmds (uninstall-build-deps variant)]
    (-> [(format "ENV BOOT_VERSION=%s" build-tool-version)
         "ENV BOOT_INSTALL=/usr/local/bin/"
         ""
         "WORKDIR /tmp"
         ""
         "# NOTE: BOOT_VERSION tells the boot.sh script which version of boot to install"
         "# on its first run. We always download the latest version of boot.sh because"
         "# it is just the installer script."
         "RUN \\"]
        (concat-commands install-dep-cmds)
        (concat-commands
          ["mkdir -p $BOOT_INSTALL"
           "wget -q https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh"
           "echo \"Comparing installer checksum...\""
           "sha256sum boot.sh"
           "echo \"0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3 *boot.sh\" | sha256sum -c -"
           "mv boot.sh $BOOT_INSTALL/boot"
           "chmod 0755 $BOOT_INSTALL/boot"] (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (concat
          [""
           "ENV PATH=$PATH:$BOOT_INSTALL"
           "ENV BOOT_AS_ROOT=yes"
           ""
           "RUN boot"])

        (->> (remove nil?)))))

(def entrypoint
  ["ENTRYPOINT [\"boot\"]"])

(def command
  ["CMD [\"repl\"]"])

(defn contents [variant]
  (concat (install variant) [""] entrypoint command))