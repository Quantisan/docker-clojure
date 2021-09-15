FROM openjdk:17-buster

ENV BOOT_VERSION=2.8.3
ENV BOOT_INSTALL=/usr/local/bin/

WORKDIR /tmp

# NOTE: BOOT_VERSION tells the boot.sh script which version of boot to install
# on its first run. We always download the latest version of boot.sh because
# it is just the installer script.
RUN \
mkdir -p $BOOT_INSTALL && \
wget -q https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh && \
echo "Comparing installer checksum..." && \
sha256sum boot.sh && \
echo "0ccd697f2027e7e1cd3be3d62721057cbc841585740d0aaa9fbb485d7b1f17c3 *boot.sh" | sha256sum -c - && \
mv boot.sh $BOOT_INSTALL/boot && \
chmod 0755 $BOOT_INSTALL/boot

ENV PATH=$PATH:$BOOT_INSTALL
ENV BOOT_AS_ROOT=yes

RUN boot

ENTRYPOINT ["boot"]
CMD ["repl"]
