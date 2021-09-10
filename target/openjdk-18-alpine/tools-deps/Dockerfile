FROM openjdk:18-alpine

ENV CLOJURE_VERSION=1.10.3.967

WORKDIR /tmp

RUN \
apk add --no-cache curl bash make git && \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
sha256sum linux-install-$CLOJURE_VERSION.sh && \
echo "d1fba0cd0733b7cb66e47620845ecedfd757a9bf84e8b276fdb37ed9c272d3ae *linux-install-$CLOJURE_VERSION.sh" | sha256sum -c - && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)" && \
apk del curl

# Docker bug makes rlwrap crash w/o short sleep first
# Bug: https://github.com/moby/moby/issues/28009
# As of 2021-09-10 this bug still exists, despite that issue being closed
RUN mv /usr/bin/rlwrap /usr/bin/rlwrap.real && \
echo "#!/bin/sh\n\nsleep 0.1 && exec /usr/bin/rlwrap.real \"\$@\"\n" > /usr/bin/rlwrap && \
chmod +x /usr/bin/rlwrap

ENTRYPOINT ["clj"]
CMD ["--repl"]