(ns docker-clojure.dockerfile.boot)

(defn install-deps [{:keys [distro]}]
  (case distro
    "slim-buster"
    ["RUN apt-get update && apt-get install -y wget"]

    "alpine"
    ["RUN apk add --update --no-cache bash openssl"]

    nil))

(defn contents [{:keys [build-tool-version] :as variant}]
  (-> [(format "ENV BOOT_VERSION=%s" build-tool-version)
       "ENV BOOT_INSTALL=/usr/local/bin/"
       ""
       "WORKDIR /tmp"
       ""]
      (concat (install-deps variant))
      (concat
       [""
        "# NOTE: BOOT_VERSION tells the boot.sh script which version of boot to install"
        "# on its first run. We always download the latest version of boot.sh because"
        "# it is just the installer script."
        "RUN mkdir -p $BOOT_INSTALL \\"
        "  && wget -q https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh \\"
        "  && echo \"Comparing installer checksum...\" \\"
        "  && echo \"f717ef381f2863a4cad47bf0dcc61e923b3d2afb *boot.sh\" | sha1sum -c - \\"
        "  && mv boot.sh $BOOT_INSTALL/boot \\"
        "  && chmod 0755 $BOOT_INSTALL/boot"
        ""
        "ENV PATH=$PATH:$BOOT_INSTALL"
        "ENV BOOT_AS_ROOT=yes"
        ""
        "RUN boot"
        ""
        "CMD [\"boot\", \"repl\"]"])

      (->> (remove nil?))))
