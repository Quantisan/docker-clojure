(ns docker-clojure.dockerfile.tools-deps
  (:require [docker-clojure.dockerfile.shared
             :refer [concat-commands copy-resource-file! entrypoint
                     install-distro-deps uninstall-distro-build-deps]]))

(defn prereqs [dir _variant]
  (copy-resource-file! dir "clj.sh" identity
                       #(.setExecutable % true false)))

(def distro-deps
  {:debian-slim {:build   #{"curl"}
                 :runtime #{"rlfe" "make" "git"}}
   :debian      {:build   #{"curl"}
                 :runtime #{"rlfe" "make" "git"}}
   :ubuntu      {:build   #{}
                 ;; install curl as a runtime dep b/c we need it at build time
                 ;; but upstream includes it so we don't want to uninstall it
                 :runtime #{"rlfe" "make" "git" "curl"}}
   :alpine      {:build   #{"curl"}
                 :runtime #{"bash" "make" "git"}}})

(def install-deps (partial install-distro-deps distro-deps))

(def uninstall-build-deps (partial uninstall-distro-build-deps distro-deps))

(def docker-bug-notice
  ["# Custom \"clj\" script that uses rlfe instead of rlwrap"])

(defn install [installer-hashes {:keys [build-tool-version] :as variant}]
  (let [install-dep-cmds   (install-deps variant)
        uninstall-dep-cmds (uninstall-build-deps variant)]
    (-> [(format "ENV CLOJURE_VERSION=%s" build-tool-version)
         ""
         "WORKDIR /tmp"
         ""
         "RUN \\"]
        (concat-commands install-dep-cmds)
        (concat-commands
          ["curl -fsSLO https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh"
           "sha256sum linux-install-$CLOJURE_VERSION.sh"
           (str "echo \"" (get-in installer-hashes ["tools-deps" build-tool-version]) " *linux-install-$CLOJURE_VERSION.sh\" | sha256sum -c -")
           "chmod +x linux-install-$CLOJURE_VERSION.sh"
           "./linux-install-$CLOJURE_VERSION.sh"
           "rm linux-install-$CLOJURE_VERSION.sh"
           "clojure -e \"(clojure-version)\""] (empty? uninstall-dep-cmds))
        (concat-commands uninstall-dep-cmds :end)
        (concat [""] docker-bug-notice
                ["COPY clj.sh /usr/local/bin/clj"])
        (->> (remove nil?)))))

(defn command [{:keys [jdk-version]}]
  (if (>= jdk-version 16)
    ["CMD [\"-M\", \"--repl\"]"]
    [(str "CMD [\"clj\"]")]))

(defn contents [installer-hashes variant]
  (concat (install installer-hashes variant)
          [""]
          (entrypoint variant)
          (command variant)))
