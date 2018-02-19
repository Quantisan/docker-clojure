#!/usr/bin/env bash

set -e
shopt -s nullglob

if ((BASH_VERSINFO[0] < 4)); then
  echo "You need bash version 4+ to run this script"
  exit 1
fi

./update.sh
./build-images.sh
./test-images.sh
./push-images.sh
