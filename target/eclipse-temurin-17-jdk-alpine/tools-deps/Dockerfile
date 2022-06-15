FROM eclipse-temurin:17-jdk-alpine

ENV CLOJURE_VERSION=1.11.1.1129

WORKDIR /tmp

RUN \
apk add --no-cache curl bash make git && \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "02bec16a9bc0a7a1a7912ca72fc8042f46e106369e572ca7185a8522e8f40123 *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
rm linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apk del curl

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2021-09-10 this bug still exists, despite that issue being closed
COPY rlwrap.retry /usr/local/bin/rlwrap

COPY entrypoint /usr/local/bin/entrypoint

ENTRYPOINT ["entrypoint"]
CMD ["-M", "--repl"]
