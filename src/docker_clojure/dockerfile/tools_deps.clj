(ns docker-clojure.dockerfile.tools-deps
  (:require [docker-clojure.dockerfile.shared :refer :all]
            [clojure.string :as str]))

(def distro-deps
  {"slim-buster" {:build   #{"wget" "curl"}
                  :runtime #{"rlwrap" "make"}}
   "buster"      {:build   #{}
                  :runtime #{"rlwrap" "make"}}
   "alpine"      {:build   #{"curl"}
                  :runtime #{"bash" "make"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

(defn install [{:keys [build-tool-version] :as variant}]
  (let [install-dep-cmds   (install-deps variant)
        uninstall-dep-cmds (uninstall-build-deps variant)]
    (-> [(format "ENV CLOJURE_VERSION=%s" build-tool-version)
         ""
         "WORKDIR /tmp"
         ""
         "RUN \\"]
        (concat-commands install-dep-cmds)
        (concat-commands
          ["wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh"
           "sha256sum linux-install-$CLOJURE_VERSION.sh"
           "echo \"7f9e4e7c5a8171db4e4edf5ce78e5b8f453bae641a4c6b7f3dda36c3128d2ff7 *linux-install-$CLOJURE_VERSION.sh\" | sha256sum -c -"
           "chmod +x linux-install-$CLOJURE_VERSION.sh"
           "./linux-install-$CLOJURE_VERSION.sh"
           "clojure -e \"(clojure-version)\""] (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)

        (->> (remove nil?)))))

(def command
  ["# Docker bug makes rlwrap crash w/o short sleep first"
   "# Bug: https://github.com/moby/moby/issues/28009"
   "# As of 2019-10-2 this bug still exists, despite that issue being closed"
   "CMD [\"sh\", \"-c\", \"sleep 1 && exec clj\"]"])

(defn contents [variant]
  (concat (install variant) [""] command))
