FROM openjdk:11
LABEL maintainer="Wes Morgan <wesmorgan@icloud.com>"

ENV BOOT_VERSION=2.8.2
ENV BOOT_INSTALL=/usr/local/bin/

WORKDIR /tmp


# NOTE: BOOT_VERSION tells the boot.sh script which version of boot to install
# on its first run. We always download the latest version of boot.sh because
# it is just the installer script.
RUN mkdir -p $BOOT_INSTALL \
  && wget -q https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh \
  && echo "Comparing installer checksum..." \
  && echo "f717ef381f2863a4cad47bf0dcc61e923b3d2afb *boot.sh" | sha1sum -c - \
  && mv boot.sh $BOOT_INSTALL/boot \
  && chmod 0755 $BOOT_INSTALL/boot

ENV PATH=$PATH:$BOOT_INSTALL
ENV BOOT_AS_ROOT=yes

RUN boot

CMD ["boot", "repl"]