FROM debian:trixie-slim

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:25 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ENV LEIN_VERSION=2.13.0
ENV LEIN_INSTALL=/usr/local/bin/

WORKDIR /tmp

# No standalone uberjar is published for this version, so build it from source.
# Verify Leiningen's GPG-signed release tag, then bootstrap leiningen-core's
# dependencies with Maven and run `lein uberjar`.
RUN set -eux; \
apt-get update && \
apt-get install -y maven git gnupg && \
rm -rf /var/lib/apt/lists/* && \
export GNUPGHOME="$(mktemp -d)" && \
export LEIN_ROOT=1 && \
mkdir -p $LEIN_INSTALL /usr/share/java /root/.lein && \
gpg --batch --keyserver hkps://keyserver.ubuntu.com --recv-keys 9D13D9426A0814B3373CF5E3D8A8243577A7859F && \
git clone --depth 1 --branch $LEIN_VERSION https://codeberg.org/leiningen/leiningen.git && \
cd leiningen && \
git verify-tag $LEIN_VERSION && \
( cd leiningen-core && mvn -B -q -DskipTests install && mvn -B -q dependency:build-classpath -Dmdep.outputFile=.lein-bootstrap ) && \
bin/lein uberjar && \
install -m 0644 target/leiningen-$LEIN_VERSION-standalone.jar /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar && \
install -m 0755 bin/lein-pkg $LEIN_INSTALL/lein && \
gpgconf --kill all && \
cd /tmp && \
rm -rf /tmp/leiningen /root/.m2 "$GNUPGHOME" && \
apt-get purge -y --auto-remove maven git gnupg

ENV PATH=$PATH:$LEIN_INSTALL
ENV LEIN_ROOT=1

# Install clojure 1.12.5 so users don't have to download it every time
RUN echo '(defproject dummy "" :dependencies [[org.clojure/clojure "1.12.5"]])' > project.clj \
  && lein deps && rm project.clj

COPY entrypoint /usr/local/bin/entrypoint

ENTRYPOINT ["entrypoint"]
CMD ["repl"]
