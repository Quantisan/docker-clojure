FROM java:7
MAINTAINER Paul Lam <paul@quantisan.com>

ENV LEIN_VERSION=2.5.1
ENV LEIN_INSTALL=/home/leinuser/bin

WORKDIR /tmp

# Add a non-root user
RUN adduser --disabled-password --gecos "" leinuser \
	&& mkdir -p $LEIN_INSTALL \

# Download the whole repo as an archive
	&& wget --quiet https://github.com/technomancy/leiningen/archive/$LEIN_VERSION.tar.gz \
	&& echo "Comparing archive checksum ..." \
	&& echo "4f6e2e189be0a163f400c3a8060896285fe731f7 *$LEIN_VERSION.tar.gz" | sha1sum -c - \
	&& tar -xzf $LEIN_VERSION.tar.gz \

# Set to use the lein-pkg script
	&& cp leiningen-$LEIN_VERSION/bin/lein-pkg $LEIN_INSTALL/lein \
	&& chmod 0755 $LEIN_INSTALL/lein \

# Download and verify Lein stand-alone jar
	&& wget --quiet https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip \
	&& wget --quiet https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip.asc \

	&& gpg --keyserver pool.sks-keyservers.net --recv-keys \
		296F37E451F91ED1783E402792893DA43FC33005 \
	&& echo "Verifying Jar file signature ..." \
	&& gpg --verify leiningen-$LEIN_VERSION-standalone.zip.asc \

# Put the jar where lein script expects
	&& cp leiningen-$LEIN_VERSION-standalone.zip /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar

USER leinuser
WORKDIR /home/leinuser
ENV PATH=$PATH:$LEIN_INSTALL

RUN lein
