#!/usr/bin/env bash
set -e
shopt -s nullglob

if ((BASH_VERSINFO[0] < 4)); then
  echo "You need bash version 4+ to run this script"
  exit 1
fi

# config parameters

OPENJDK_VERSION=8

PAUL='Paul Lam <paul@quantisan.com>'
WES='Wes Morgan <wesmorgan@icloud.com>'
DLG='Kirill Chernyshov <delaguardo@gmail.com>'

declare -A maintainers=(
  [debian/lein]=$PAUL
  [debian/boot]=$WES
  [alpine]=$WES
  [alpine/tools-deps]=$DLG
  [debian/tools-deps]=$DLG
)

# Dockerfile generator

source ./variants.sh

generated_warning() {
  cat <<EOH
#
# NOTE: THIS DOCKERFILE IS GENERATED VIA "update.sh"
#
# PLEASE DO NOT EDIT IT DIRECTLY.
#

EOH
}

get_sed_inplace_argument() {
    echo "foo" > /tmp/sed_check
    if sed -i"" "s/foo/bar/g" /tmp/sed_check 2>/dev/null; then
        echo "-i .bak"
    else
        echo "-i.bak"
    fi
    rm /tmp/sed_check
}

for variant in "${variants[@]}"; do
  openjdk_version=$OPENJDK_VERSION
  dir="$variant"
  bv=$(base_variant $variant)
  bt=$(build_tool $variant)
  bt_version=$(build_tool_version $bt)
  echo "Generating Dockerfile for $dir"
  [ -d "$dir" ] || continue
  template="Dockerfile-$bt.template"
  #echo "Using template $template"
  if [ "$bv" != "debian" ]; then
    openjdk_version="${openjdk_version}-${bv}"
  fi
  sed_inplace_arg=$(get_sed_inplace_argument)
  maintainer=${maintainers[$variant]:-${maintainers[$bv]}}
  { generated_warning; cat "$template"; } > "$dir/Dockerfile"
  ( sed "$sed_inplace_arg" 's!%%BASE_TAG%%!'"$openjdk_version"'!g' "$dir/Dockerfile"
    sed "$sed_inplace_arg" 's!%%MAINTAINER%%!'"$maintainer"'!g' "$dir/Dockerfile"
    sed "$sed_inplace_arg" 's!%%BUILD_TOOL_VERSION%%!'"$bt_version"'!g' "$dir/Dockerfile"
    if [ "$bv" = "alpine" ]; then
      sed "$sed_inplace_arg" 's/^%%ALPINE%% //g' "$dir/Dockerfile"
    else
        sed "$sed_inplace_arg" '/^%%ALPINE%%/d' "$dir/Dockerfile"
        sed "$sed_inplace_arg" '/^$/N;/^\n$/D' "$dir/Dockerfile"
    fi
  )
  find . -name "*.bak" -type f -delete
done
