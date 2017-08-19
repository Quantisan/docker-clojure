#
# NOTE: THIS DOCKERFILE IS GENERATED VIA "update.sh"
#
# PLEASE DO NOT EDIT IT DIRECTLY.
#

FROM openjdk:8
MAINTAINER Wes Morgan <wesmorgan@icloud.com>

ENV BOOT_VERSION=2.7.2
ENV BOOT_INSTALL=/usr/local/bin/

WORKDIR /tmp

# NOTE: BOOT_VERSION tells the boot.sh script which version of boot to install
# on its first run. We always download version 2.5.2 of boot.sh because it is
# just the installer script. When/if the boot project releases a new installer
# script we will update this to use it.
RUN mkdir -p $BOOT_INSTALL \
  && wget -q https://github.com/boot-clj/boot-bin/releases/download/2.5.2/boot.sh \
  && echo "Comparing installer checksum..." \
  && echo "d9cbefc6cbf043361a58b416e6d62fc80e5ead32 *boot.sh" | sha1sum -c - \
  && mv boot.sh $BOOT_INSTALL/boot \
  && chmod 0755 $BOOT_INSTALL/boot

ENV PATH=$PATH:$BOOT_INSTALL
ENV BOOT_AS_ROOT=yes

RUN boot
