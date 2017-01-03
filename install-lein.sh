#!/bin/sh

set -e

# Download the whole repo as an archive
wget -q https://github.com/technomancy/leiningen/archive/$TOOL_VERSION.tar.gz
echo "Comparing archive checksum ..."
echo "876221e884780c865c2ce5c9aa5675a7cae9f215 *$TOOL_VERSION.tar.gz" | sha1sum -c -

mkdir ./leiningen
tar -xzf $TOOL_VERSION.tar.gz  -C ./leiningen/ --strip-components=1
mv leiningen/bin/lein-pkg $TOOL_INSTALL/lein
rm -rf $TOOL_VERSION.tar.gz ./leiningen

chmod 0755 $TOOL_INSTALL/lein

# Download and verify Lein stand-alone jar
wget -q https://github.com/technomancy/leiningen/releases/download/$TOOL_VERSION/leiningen-$TOOL_VERSION-standalone.zip
wget -q https://github.com/technomancy/leiningen/releases/download/$TOOL_VERSION/leiningen-$TOOL_VERSION-standalone.zip.asc

gpg --keyserver pool.sks-keyservers.net --recv-key 2E708FB2FCECA07FF8184E275A92E04305696D78
echo "Verifying Jar file signature ..."
gpg --verify leiningen-$TOOL_VERSION-standalone.zip.asc

# Put the jar where lein script expects
rm leiningen-$TOOL_VERSION-standalone.zip.asc
mkdir -p /usr/share/java
mv leiningen-$TOOL_VERSION-standalone.zip /usr/share/java/leiningen-$TOOL_VERSION-standalone.jar
