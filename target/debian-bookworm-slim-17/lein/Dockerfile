FROM debian:bookworm-slim

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:17 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ENV LEIN_VERSION=2.11.2
ENV LEIN_INSTALL=/usr/local/bin/
ENV LEIN_ROOT=1
ENV PATH=$PATH:$LEIN_INSTALL
ENV CLOJURE_CLI_VERSION=1.11.1.1435

WORKDIR /tmp

RUN set -eux; \
apt-get update && \
apt-get install -y curl make git rlwrap gnupg && \
rm -rf /var/lib/apt/lists/* && \
echo Installing Leiningen && \
mkdir -p $LEIN_INSTALL && \
curl -fsSLO https://codeberg.org/leiningen/leiningen/raw/tag/$LEIN_VERSION/bin/lein-pkg && \
echo "Comparing lein-pkg checksum ..." && \
sha256sum lein-pkg && \
echo "28a1a62668c5f427b413a8677e376affaa995f023b1fcd06e2d4c98ac1df5f3e *lein-pkg" | sha256sum -c - && \
mv lein-pkg $LEIN_INSTALL/lein && \
chmod 0755 $LEIN_INSTALL/lein && \
export GNUPGHOME="$(mktemp -d)" && \
gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys 9D13D9426A0814B3373CF5E3D8A8243577A7859F && \
curl -fsSLO https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.jar && \
curl -fsSLO https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.jar.asc && \
echo "Verifying Leiningen file PGP signature..." && \
gpg --batch --verify leiningen-$LEIN_VERSION-standalone.jar.asc leiningen-$LEIN_VERSION-standalone.jar && \
gpgconf --kill all && \
rm -rf "$GNUPGHOME" leiningen-$LEIN_VERSION-standalone.jar.asc && \
mkdir -p /usr/share/java && \
mv leiningen-$LEIN_VERSION-standalone.jar /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar && \
mkdir -p ~/.lein/ && \
echo Installing tools.deps && \
curl -fsSLO https://download.clojure.org/install/linux-install-$CLOJURE_CLI_VERSION.sh && \
sha256sum linux-install-$CLOJURE_CLI_VERSION.sh && \
echo "7edee5b12197a2dbe6338e672b109b18164cde84bea1f049ceceed41fc4dd10a *linux-install-$CLOJURE_CLI_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_CLI_VERSION.sh && \
./linux-install-$CLOJURE_CLI_VERSION.sh && \
rm linux-install-$CLOJURE_CLI_VERSION.sh && \
clojure -e "(clojure-version)" && \
apt-get purge -y --auto-remove curl gnupg

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2021-09-10 this bug still exists, despite that issue being closed
COPY rlwrap.retry /usr/local/bin/rlwrap

COPY entrypoint /usr/local/bin/entrypoint

ENTRYPOINT ["entrypoint"]
CMD ["repl"]
