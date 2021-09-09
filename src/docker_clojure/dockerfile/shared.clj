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

(defn get-deps [type distro-deps distro]
  (some->> distro namespace keyword (get distro-deps) type))

(def build-deps (partial get-deps :build))

(def runtime-deps (partial get-deps :runtime))

(defn all-deps [distro-deps distro]
  (set (concat (build-deps distro-deps distro)
               (runtime-deps distro-deps distro))))

(defn install-distro-deps [distro-deps {:keys [distro]}]
  (let [deps (all-deps distro-deps distro)]
    (when (seq deps)
      (case (-> distro namespace keyword)
        (:debian :debian-slim)
        ["apt-get update"
         (str/join " " (concat ["apt-get install -y"] deps))
         "rm -rf /var/lib/apt/lists/*"]

        :alpine
        [(str/join " " (concat ["apk add --no-cache"] deps))]

        nil))))

(defn uninstall-distro-build-deps [distro-deps {:keys [distro]}]
  (let [deps (build-deps distro-deps distro)]
    (when (seq deps)
      (case (-> distro namespace keyword)
        (:debian :debian-slim)
        [(str/join " " (concat ["apt-get purge -y --auto-remove"] deps))]

        :alpine
        [(str/join " " (concat ["apk del"] deps))]

        nil))))