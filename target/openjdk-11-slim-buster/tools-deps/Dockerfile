FROM openjdk:11-slim-buster
LABEL maintainer="Paul Lam <paul@quantisan.com> & Wes Morgan <wesmorgan@icloud.com>"

ENV CLOJURE_VERSION=1.10.1.469

WORKDIR /tmp

RUN apt-get update && apt-get install -y wget curl

RUN wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh \
    && chmod +x linux-install-$CLOJURE_VERSION.sh \
    && ./linux-install-$CLOJURE_VERSION.sh

RUN clojure -e "(clojure-version)"

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
CMD ["sh", "-c", "sleep 1 && exec clj"]