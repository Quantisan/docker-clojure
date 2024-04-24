(ns docker-clojure.dockerfile.shared
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(defn multiline-RUN [commands]
  (concat ["RUN set -eux; \\"]
          (map #(str % " && \\") (butlast commands))
          [(last commands)]))

(defn get-deps [type distro-deps distro]
  (some->> distro namespace keyword (get distro-deps) type))

(def build-deps (partial get-deps :build))

(def runtime-deps (partial get-deps :runtime))

(defn all-deps [distro-deps distro]
  (set (concat (build-deps distro-deps distro)
               (runtime-deps distro-deps distro))))

(defn- cmd [& args]
  (str/join " " (flatten args)))

(defn install-distro-deps [distro-deps {:keys [distro]}]
  (when-let [deps (seq (all-deps distro-deps distro))]
    (case (-> distro namespace keyword)
      (:debian :debian-slim :ubuntu)
      ["apt-get update"
       (cmd "apt-get install -y" deps)
       "rm -rf /var/lib/apt/lists/*"]

      :alpine
      [(cmd "apk add --no-cache" deps)])))

(defn uninstall-distro-build-deps [distro-deps {:keys [distro]}]
  (when-let [deps (seq (build-deps distro-deps distro))]
    (case (-> distro namespace keyword)
      (:debian :debian-slim :ubuntu)
      [(cmd "apt-get purge -y --auto-remove" deps)]

      :alpine
      [(cmd "apk del" deps)])))

(defn copy-resource-file!
  "Copy a file named `filename` from resources to a specified `build-dir`.
  The file contents will be passed to the `processor` fn and whatever that
  returns used in the image (default processor is `identity`)."
  ([build-dir filename]
   (copy-resource-file! build-dir filename identity identity))
  ([build-dir filename contents-processor]
   (copy-resource-file! build-dir filename contents-processor identity))
  ([build-dir filename contents-processor file-processor]
   (let [src  (-> filename io/resource io/file)
         dest (io/file build-dir filename)]
     (->> src slurp contents-processor (spit dest))
     (file-processor dest))))

(defn entrypoint
  "This is the same for every build-tool so far, so it's in here. If that
  changes move it into the build-tool-specific namespaces (or future protocol)."
  [{:keys [jdk-version]}]
  (when (>= jdk-version 16)
    ["COPY entrypoint /usr/local/bin/entrypoint"
     ""
     "ENTRYPOINT [\"entrypoint\"]"]))
