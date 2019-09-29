FROM openjdk:14-buster

ENV CLOJURE_VERSION=1.10.1.469

WORKDIR /tmp

RUN \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)"

CMD ["sh", "-c", "sleep 1 && exec clj"]