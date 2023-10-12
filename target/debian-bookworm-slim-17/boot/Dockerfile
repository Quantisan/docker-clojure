FROM debian:bookworm-slim

ENV JAVA_HOME=/opt/java/openjdk
COPY --from=eclipse-temurin:17 $JAVA_HOME $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

ENV BOOT_VERSION=2.8.3
ENV BOOT_INSTALL=/usr/local/bin/

WORKDIR /tmp

# NOTE: BOOT_VERSION tells the boot.sh script which version of boot to install
# on its first run. We always download the latest version of boot.sh because
# it is just the installer script.
RUN \
apt-get update && \
apt-get install -y wget && \
rm -rf /var/lib/apt/lists/* && \
mkdir -p $BOOT_INSTALL && \
wget -q https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh && \
echo "Comparing installer checksum..." && \
sha256sum boot.sh && \
echo "0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3 *boot.sh" | sha256sum -c - && \
mv boot.sh $BOOT_INSTALL/boot && \
chmod 0755 $BOOT_INSTALL/boot && \
apt-get purge -y --auto-remove wget

ENV PATH=$PATH:$BOOT_INSTALL
ENV BOOT_AS_ROOT=yes

RUN boot

COPY entrypoint /usr/local/bin/entrypoint

ENTRYPOINT ["entrypoint"]
CMD ["repl"]
