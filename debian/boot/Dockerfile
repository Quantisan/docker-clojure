FROM openjdk:8
MAINTAINER Wes Morgan <wesmorgan@icloud.com>

ENV BOOT_VERSION=2.7.1
ENV BOOT_INSTALL=/usr/local/bin/

WORKDIR /tmp

RUN mkdir -p $BOOT_INSTALL \
  && wget -q https://github.com/boot-clj/boot-bin/releases/download/2.5.2/boot.sh \
  && echo "Comparing installer checksum..." \
  && echo "d9cbefc6cbf043361a58b416e6d62fc80e5ead32 *boot.sh" | sha1sum -c - \
  && mv boot.sh $BOOT_INSTALL/boot \
  && chmod 0755 $BOOT_INSTALL/boot

ENV PATH=$PATH:$BOOT_INSTALL
ENV BOOT_AS_ROOT=yes

RUN boot
