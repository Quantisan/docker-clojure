FROM eclipse-temurin:8-jdk-alpine

ENV CLOJURE_VERSION=1.12.2.1565

WORKDIR /tmp

RUN \
apk add --no-cache curl bash make git rlwrap && \
curl -fsSLO https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "68442caaeaaa0780957953dfac11278e3991d3baeb22579fc582ed1b2d5cd152 *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
rm linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apk del curl

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2021-09-10 this bug still exists, despite that issue being closed
COPY rlwrap.retry /usr/local/bin/rlwrap

CMD ["clj"]
