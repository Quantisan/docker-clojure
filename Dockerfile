FROM java:7
MAINTAINER Paul Lam <paul@quantisan.com>

ENV LEIN_VERSION=2.5.1
ENV LEIN_INSTALL=/home/leinuser/bin

RUN adduser --disabled-password --gecos "" leinuser
RUN mkdir -p $LEIN_INSTALL

WORKDIR /tmp

# Download the whole repo as an archive
RUN wget --quiet https://github.com/technomancy/leiningen/archive/$LEIN_VERSION.tar.gz
RUN echo "Comparing archive checksum ..."
RUN echo "4f6e2e189be0a163f400c3a8060896285fe731f7 *$LEIN_VERSION.tar.gz" | sha1sum -c -
RUN tar -xzf $LEIN_VERSION.tar.gz

# Set to use the lein-pkg script
RUN cp leiningen-$LEIN_VERSION/bin/lein-pkg $LEIN_INSTALL/lein
RUN chmod 0755 $LEIN_INSTALL/lein

# Download and verify Lein stand-alone jar
RUN wget --quiet https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip
RUN wget --quiet https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip.asc

RUN gpg --keyserver pool.sks-keyservers.net --recv-keys \
	296F37E451F91ED1783E402792893DA43FC33005
RUN echo "Verifying Jar file signature ..."
RUN gpg --verify leiningen-$LEIN_VERSION-standalone.zip.asc

# Put the jar where lein script expects
RUN cp leiningen-$LEIN_VERSION-standalone.zip /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar

USER leinuser
WORKDIR /home/leinuser
ENV PATH $PATH:$LEIN_INSTALL

RUN lein
