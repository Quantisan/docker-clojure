#!/usr/bin/env bash

set -e
shopt -s nullglob

if ((BASH_VERSINFO[0] < 4)); then
  echo "You need bash version 4+ to run this script"
  exit 1
fi

source ./variants.sh

for variant in "${variants[@]}"; do
  image_name=$(image_name $variant)
  echo "Pushing $image_name"
  docker push $image_name
done
