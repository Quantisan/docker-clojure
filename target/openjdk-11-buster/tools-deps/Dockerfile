FROM openjdk:11-buster

ENV CLOJURE_VERSION=1.10.1.716

WORKDIR /tmp

RUN \
apt-get update && \
apt-get install -y make rlwrap && \
rm -rf /var/lib/apt/lists/* && \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "6900337e2210895130394bb93248465f7fb9a427fec46638636398bbe82842e3 *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)"

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2019-10-2 this bug still exists, despite that issue being closed
CMD ["sh", "-c", "sleep 1 && exec clj"]