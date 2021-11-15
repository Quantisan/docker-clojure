(ns docker-clojure.dockerfile.shared
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

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

(defn copy-resource-file 
  "Copy a file named `filename` from resources to a dir in the image
  `image-dir`. The file contents will be passed to the `processor`
  fn and whatever that returns used in the image (default processor is
  `identity`)."
  ([image-dir filename] (copy-resource-file image-dir filename identity))
  ([image-dir filename processor]
   (let [src  (-> filename io/resource io/file)
         dest (io/file image-dir filename)]
     (->> src slurp processor (spit dest)))))
