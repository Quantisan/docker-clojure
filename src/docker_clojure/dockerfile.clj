(ns docker-clojure.dockerfile
  (:require
    [clojure.java.shell :refer [sh]]
    [clojure.string :as str]
    [docker-clojure.dockerfile.boot :as boot]
    [docker-clojure.dockerfile.lein :as lein]
    [docker-clojure.dockerfile.tools-deps :as tools-deps]
    [clojure.java.io :as io]))

(defn build-dir [{:keys [base-image build-tool]}]
  (str/join "/" ["target"
                 (-> base-image (str/replace ":" "-") (str/replace "/" "-"))
                 (if (= :docker-clojure.core/all build-tool)
                   "latest"
                   build-tool)]))

(defn all-contents [variant]
  (concat
    ["### INSTALL BOOT ###"]
    (boot/install
     (assoc variant :build-tool-version
            (get-in variant [:build-tool-versions "boot"])))
    ["" "### INSTALL LEIN ###"]
    (lein/install
     (assoc variant :build-tool-version
            (get-in variant [:build-tool-versions "lein"])))
    ["" "### INSTALL TOOLS-DEPS ###"]
    (tools-deps/install
     (assoc variant :build-tool-version
            (get-in variant [:build-tool-versions "tools-deps"])))
    ["" "CMD [\"lein\", \"repl\"]"]))

(defn path-after [dir file]
  (-> file .getPath (str/split (re-pattern (str "/" dir "/"))) second))

;; TODO: Generalize the file copying stuff if any other images ever need it
(defn base-image-contents [{:keys [base-image build-dir]}]
  (cond
    (str/includes? base-image "graalvm")
    (concat
      (remove
        nil?
        (mapcat (fn [f]
                  (when (.isFile f)
                    (io/copy f (io/file build-dir (.getName f)))
                    [(str "COPY " (.getName f) " "
                          (str "/" (path-after "graalvm" f)))]))
                (-> "graalvm" io/resource io/file file-seq)))
      [""]
      ["RUN gu install native-image" ""])))

(defn contents [{:keys [build-tool] :as variant}]
  (str/join "\n"
            (remove nil?
                    (concat [(format "FROM %s" (:base-image variant)) ""]
                            (base-image-contents variant)
                            (case build-tool
                              :docker-clojure.core/all (all-contents variant)
                              "boot" (boot/contents variant)
                              "lein" (lein/contents variant)
                              "tools-deps" (tools-deps/contents variant))))))

(defn write-file [dir file variant]
  (let [{:keys [exit err]} (sh "mkdir" "-p" dir)]
    (if (zero? exit)
      (spit (str/join "/" [dir file])
            (contents variant))
      (throw (ex-info (str "Error creating directory " dir)
                      {:error err})))))

(defn clean-all []
  (sh "sh" "-c" "rm -rf target/*"))
