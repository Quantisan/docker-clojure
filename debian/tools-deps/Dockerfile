#
# NOTE: THIS DOCKERFILE IS GENERATED VIA "update.sh"
#
# PLEASE DO NOT EDIT IT DIRECTLY.
#

FROM openjdk:8
LABEL mainteiner="Kirill Chernyshov <delaguardo@gmail.com>"

ENV CLOJURE_VERSION=1.9.0.358

WORKDIR /tmp

RUN wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh

RUN clojure -e "(clojure-version)"