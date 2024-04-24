FROM eclipse-temurin:8-jdk-jammy

ENV CLOJURE_VERSION=1.11.1.1435

WORKDIR /tmp

RUN \
apt-get update && \
apt-get install -y curl make git rlfe && \
rm -rf /var/lib/apt/lists/* && \
curl -fsSLO https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "7edee5b12197a2dbe6338e672b109b18164cde84bea1f049ceceed41fc4dd10a *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
rm linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)"

# Custom "clj" script that uses rlfe instead of rlwrap
COPY clj.sh /usr/local/bin/clj

CMD ["clj"]
