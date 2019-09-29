FROM openjdk:14-alpine

ENV CLOJURE_VERSION=1.10.1.469

WORKDIR /tmp

RUN \
apk add --update --no-cache curl bash && \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apk del curl bash

CMD ["sh", "-c", "sleep 1 && exec clj"]