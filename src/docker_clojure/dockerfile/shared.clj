(ns docker-clojure.dockerfile.shared)

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