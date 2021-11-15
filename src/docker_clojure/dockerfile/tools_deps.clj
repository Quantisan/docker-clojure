(ns docker-clojure.dockerfile.tools-deps
  (:require [docker-clojure.dockerfile.shared :refer :all]))

(defn prereqs [dir _variant]
  (copy-resource-file dir "rlwrap.retry"))

(def distro-deps
  {:debian-slim {:build   #{"wget" "curl"}
                 :runtime #{"rlwrap" "make" "git"}}
   :debian      {:build   #{}
                 :runtime #{"rlwrap" "make"}}
   :alpine      {:build   #{"curl"}
                 :runtime #{"bash" "make" "git"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

(def docker-bug-notice
  ["# Docker bug makes rlwrap crash w/o short sleep first"
   "# Bug: https://github.com/moby/moby/issues/28009"
   "# As of 2021-09-10 this bug still exists, despite that issue being closed"])

(defn install [installer-hashes {:keys [build-tool-version jdk-version] :as variant}]
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
           (str "echo \"" (get-in installer-hashes ["tools-deps" build-tool-version]) " *linux-install-$CLOJURE_VERSION.sh\" | sha256sum -c -")
           "chmod +x linux-install-$CLOJURE_VERSION.sh"
           "./linux-install-$CLOJURE_VERSION.sh"
           "rm linux-install-$CLOJURE_VERSION.sh"
           "clojure -e \"(clojure-version)\""] (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (#(if (>= jdk-version 16) 
            (concat % [""] ["COPY entrypoint /usr/local/bin/entrypoint"])
            %))
        (concat [""] docker-bug-notice
                ["COPY rlwrap.retry /usr/bin/rlwrap.retry"])
        (concat-commands
          ["RUN mv /usr/bin/rlwrap /usr/bin/rlwrap.real"
           "mv /usr/bin/rlwrap.retry /usr/bin/rlwrap"
           "chmod +x /usr/bin/rlwrap"] :end)

        (->> (remove nil?)))))

(defn entrypoint [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    [(str "ENTRYPOINT [\"entrypoint\"]")]
    nil))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    ["CMD [\"-M\", \"--repl\"]"]
    [(str "CMD [\"clj\"]")]))

(defn contents [installer-hashes variant]
  (concat (install installer-hashes variant)
          [""]
          (entrypoint variant)
          (command variant)))
