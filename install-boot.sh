#!/bin/sh

set -e

# Download boot
wget --quiet https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh
mv boot.sh $TOOL_INSTALL/boot
chmod +x $TOOL_INSTALL/boot

# Run REPL to install nrepl deps
$TOOL_INSTALL/boot repl -e '(System/exit 0)'
