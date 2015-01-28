FROM java:7
MAINTAINER Paul Lam <paul@quantisan.com>


RUN curl -s https://raw.githubusercontent.com/technomancy/leiningen/2.5.1/bin/lein > \
            /usr/local/bin/lein && \
            chmod 0755 /usr/local/bin/lein
ENV LEIN_ROOT 1
RUN lein