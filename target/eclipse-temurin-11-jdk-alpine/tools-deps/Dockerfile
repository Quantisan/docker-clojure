FROM eclipse-temurin:11-jdk-alpine

ENV CLOJURE_VERSION=1.11.3.1456

WORKDIR /tmp

RUN \
apk add --no-cache curl bash make git && \
curl -fsSLO https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "2f5edc801133c72a49e990816b0e245beb8b4e35a85524b4dd0b3fa03a4a5365 *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
rm linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apk del curl

# Custom "clj" script that uses rlfe instead of rlwrap
COPY clj.sh /usr/local/bin/clj

CMD ["clj"]
