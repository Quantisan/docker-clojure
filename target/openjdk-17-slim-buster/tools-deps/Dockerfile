FROM openjdk:17-slim-buster

ENV CLOJURE_VERSION=1.10.3.1020

WORKDIR /tmp

RUN \
apt-get update && \
apt-get install -y curl make git rlwrap wget && \
rm -rf /var/lib/apt/lists/* && \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "afc87e2c8cfbf87e43553439c69a4c8e36bc2094405d08f39ca542b4cca0920a *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
rm linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apt-get purge -y --auto-remove curl wget

COPY entrypoint /usr/local/bin/entrypoint
RUN chmod +x /usr/local/bin/entrypoint

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2021-09-10 this bug still exists, despite that issue being closed
COPY rlwrap.retry /usr/bin/rlwrap.retry
RUN mv /usr/bin/rlwrap /usr/bin/rlwrap.real && \
mv /usr/bin/rlwrap.retry /usr/bin/rlwrap && \
chmod +x /usr/bin/rlwrap

ENTRYPOINT ["entrypoint"]
CMD ["-M", "--repl"]
