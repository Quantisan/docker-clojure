(ns docker-clojure.dockerfile.tools-deps
  (:require [docker-clojure.dockerfile.shared :refer :all]
            [clojure.string :as str]))

(def distro-deps
  {"slim-buster" {:build   #{"wget" "curl"}
                  :runtime #{"rlwrap"}}
   "alpine"      {:build   #{"bash" "curl"}
                  :runtime #{}}})

(defn install-deps [{:keys [distro]}]
  (case distro
    "slim-buster"
    ["apt-get update"
     (str/join " " (concat ["apt-get install -y"]
                           (all-deps distro-deps distro)))]

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

(defn contents [{:keys [build-tool-version] :as variant}]
  (let [install-dep-cmds (install-deps variant)
        uninstall-dep-cmds (uninstall-build-deps variant)]
    (-> [(format "ENV CLOJURE_VERSION=%s" build-tool-version)
         ""
         "WORKDIR /tmp"
         ""
         "RUN \\"]
        (concat-commands install-dep-cmds)
        (concat-commands
         ["wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh"
          "chmod +x linux-install-$CLOJURE_VERSION.sh"
          "./linux-install-$CLOJURE_VERSION.sh"
          "clojure -e \"(clojure-version)\""] (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (concat
         [""
          "CMD [\"sh\", \"-c\", \"sleep 1 && exec clj\"]"])

        (->> (remove nil?)))))

