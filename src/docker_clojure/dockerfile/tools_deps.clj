(ns docker-clojure.dockerfile.tools-deps)

(defn install-deps [{:keys [distro]}]
  (case distro
    "alpine"
    ["RUN echo '@testing http://dl-cdn.alpinelinux.org/alpine/edge/testing' >> /etc/apk/repositories && \\"
     "  apk add --update --no-cache bash curl rlwrap@testing"]

    "debian"
    ["RUN apt-get update && apt-get install -y rlwrap"]

    nil))

(defn contents [{:keys [build-tool-version] :as variant}]
  (-> [(format "ENV CLOJURE_VERSION=%s" build-tool-version)
       ""
       "WORKDIR /tmp"
       ""]
      (concat (install-deps variant))
      (concat
       [""
        "RUN wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \\"
        "    && chmod +x linux-install-$CLOJURE_VERSION.sh \\"
        "    && ./linux-install-$CLOJURE_VERSION.sh"
        ""
        "RUN clojure -e \"(clojure-version)\""
        ""
        "# Docker bug makes rlwrap crash w/o short sleep first"
        "# Bug: https://github.com/moby/moby/issues/28009"
        "CMD sleep 1; clj"])
      (->> (remove nil?))))

