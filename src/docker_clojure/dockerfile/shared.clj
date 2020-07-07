(ns docker-clojure.dockerfile.shared
  (:require [clojure.string :as str]))

(defn concat-commands [base cmds & [end?]]
  (let [commands (if end?
                   (butlast cmds)
                   cmds)]
    (concat base
           (map #(str % " && \\")
                commands)
           (when end? [(last cmds)]))))

(defn build-deps [distro-deps distro]
  (-> distro-deps (get distro) :build))

(defn runtime-deps [distro-deps distro]
  (-> distro-deps (get distro) :runtime))

(defn all-deps [distro-deps distro]
  (set (concat (build-deps distro-deps distro)
               (runtime-deps distro-deps distro))))

(defn install-distro-deps [distro-deps {:keys [distro]}]
  (let [deps (all-deps distro-deps distro)]
    (when (seq deps)
      (case distro
        ("slim-buster" "buster")
        ["apt-get update"
         (str/join " " (concat ["apt-get install -y"] deps))
         "rm -rf /var/lib/apt/lists/*"]

        "alpine"
        [(str/join " " (concat ["apk add --update --no-cache"] deps))]

        nil))))

(defn uninstall-distro-build-deps [distro-deps {:keys [distro]}]
  (let [deps (build-deps distro-deps distro)]
    (when (seq deps)
      (case distro
        ("slim-buster" "buster")
        [(str/join " " (concat ["apt-get remove -y --purge"] deps))
         "apt-get autoremove -y"]

        "alpine"
        [(str/join " " (concat ["apk del"] deps))]

        nil))))