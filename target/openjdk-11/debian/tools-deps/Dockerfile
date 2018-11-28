FROM openjdk:11
LABEL maintainer="Kirill Chernyshov <delaguardo@gmail.com>"

ENV CLOJURE_VERSION=1.9.0.397

WORKDIR /tmp

RUN apt-get update && apt-get install -y rlwrap

RUN wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh

RUN clojure -e "(clojure-version)"

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
CMD sleep 1; clj