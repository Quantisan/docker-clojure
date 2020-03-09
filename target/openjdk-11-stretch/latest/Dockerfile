FROM openjdk:11-stretch

### INSTALL BOOT ###
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
echo "f717ef381f2863a4cad47bf0dcc61e923b3d2afb *boot.sh" | sha1sum -c - && \
mv boot.sh $BOOT_INSTALL/boot && \
chmod 0755 $BOOT_INSTALL/boot

ENV PATH=$PATH:$BOOT_INSTALL
ENV BOOT_AS_ROOT=yes

RUN boot

### INSTALL LEIN ###
ENV LEIN_VERSION=2.9.2
ENV LEIN_INSTALL=/usr/local/bin/

WORKDIR /tmp

# Download the whole repo as an archive
RUN \
mkdir -p $LEIN_INSTALL && \
wget -q https://raw.githubusercontent.com/technomancy/leiningen/$LEIN_VERSION/bin/lein-pkg && \
echo "Comparing lein-pkg checksum ..." && \
sha1sum lein-pkg && \
echo "894c4b9aa425c0623cd00430934f3cd503de22b9 *lein-pkg" | sha1sum -c - && \
mv lein-pkg $LEIN_INSTALL/lein && \
chmod 0755 $LEIN_INSTALL/lein && \
wget -q https://github.com/technomancy/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.zip && \
mkdir -p /usr/share/java && \
mv leiningen-$LEIN_VERSION-standalone.zip /usr/share/java/leiningen-$LEIN_VERSION-standalone.jar

ENV PATH=$PATH:$LEIN_INSTALL
ENV LEIN_ROOT 1

# Install clojure 1.10.1 so users don't have to download it every time
RUN echo '(defproject dummy "" :dependencies [[org.clojure/clojure "1.10.1"]])' > project.clj \
  && lein deps && rm project.clj

### INSTALL TOOLS-DEPS ###
ENV CLOJURE_VERSION=1.10.1.502

WORKDIR /tmp

RUN \
wget https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh && \
chmod +x linux-install-$CLOJURE_VERSION.sh && \
./linux-install-$CLOJURE_VERSION.sh && \
clojure -e "(clojure-version)"

CMD ["lein", "repl"]