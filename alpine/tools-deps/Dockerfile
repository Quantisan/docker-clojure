#
# NOTE: THIS DOCKERFILE IS GENERATED VIA "update.sh"
#
# PLEASE DO NOT EDIT IT DIRECTLY.
#

FROM openjdk:8-alpine
LABEL maintainer="Kirill Chernyshov <delaguardo@gmail.com>"

ENV CLOJURE_VERSION=1.9.0.391

WORKDIR /tmp

RUN apk add --update bash curl && rm -rf /var/cache/apk/*

RUN wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh

RUN clojure -e "(clojure-version)"
