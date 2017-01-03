#!/bin/sh

set -e

# Download boot
wget -q https://github.com/boot-clj/boot-bin/releases/download/2.5.2/boot.sh
echo "Comparing checksum..."
echo "d9cbefc6cbf043361a58b416e6d62fc80e5ead32 *boot.sh" | sha1sum -c -

# Install boot
mv boot.sh $TOOL_INSTALL/boot
chmod +x $TOOL_INSTALL/boot
