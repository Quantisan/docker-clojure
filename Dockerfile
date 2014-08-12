FROM java
MAINTAINER Paul Lam <paul@quantisan.com>

RUN curl -s https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > \
            /usr/bin/lein && \
            chmod 0755 /usr/bin/lein
RUN lein

ENV LEIN_ROOT 1