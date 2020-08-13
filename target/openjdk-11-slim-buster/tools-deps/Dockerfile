FROM openjdk:11-slim-buster

ENV CLOJURE_VERSION=1.10.1.561

WORKDIR /tmp

RUN \
apt-get update && \
apt-get install -y curl make rlwrap wget && \
rm -rf /var/lib/apt/lists/* && \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "7f9e4e7c5a8171db4e4edf5ce78e5b8f453bae641a4c6b7f3dda36c3128d2ff7 *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apt-get purge -y --auto-remove curl wget

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2019-10-2 this bug still exists, despite that issue being closed
CMD ["sh", "-c", "sleep 1 && exec clj"]