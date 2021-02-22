FROM openjdk:17-slim-buster

ENV CLOJURE_VERSION=1.10.2.790

WORKDIR /tmp

RUN \
apt-get update && \
apt-get install -y curl make rlwrap wget && \
rm -rf /var/lib/apt/lists/* && \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "53771e15b9d15f9c87b59096d098af93430f80b7fea09602b1733b1cc62fd515 *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apt-get purge -y --auto-remove curl wget

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2019-10-2 this bug still exists, despite that issue being closed
CMD ["sh", "-c", "sleep 1 && exec clj"]