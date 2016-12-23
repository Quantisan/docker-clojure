FROM openjdk:8-alpine
MAINTAINER Wes Morgan <wesmorgan@icloud.com>

ARG BUILD_TOOL=lein
ARG TOOL_VERSION=2.7.1
ENV TOOL_INSTALL=/usr/local/bin/

WORKDIR /tmp

COPY ./install-${BUILD_TOOL}.sh install-build-tool.sh

ENV LEIN_ROOT 1
ENV BOOT_AS_ROOT yes
ENV BOOT_VERSION $TOOL_VERSION

# Install real tar as BusyBox's doesn't support --strip-components
RUN apk add --update tar gnupg bash openssl && rm -rf /var/cache/apk/*

RUN mkdir -p $TOOL_INSTALL \
  && chmod +x install-build-tool.sh \
  && ./install-build-tool.sh \
  && rm install-build-tool.sh

ENV PATH=$PATH:$TOOL_INSTALL

RUN $BUILD_TOOL
