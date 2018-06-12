#!/usr/bin/env bash
set -e
shopt -s nullglob

if ((BASH_VERSINFO[0] < 4)); then
  echo "You need bash version 4+ to run this script"
  exit 1
fi

# config parameters

#OPENJDK_VERSION=${1:-8}

declare -a JDK_VERSIONS=('8' '10')

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

for variant in "${variants[@]}"; do
    for OPENJDK_VERSION in "${JDK_VERSIONS[@]}"; do
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
        maintainer=${maintainers[$variant]:-${maintainers[$bv]}}
        { generated_warning; cat "$template"; } > "$dir/Dockerfile-$openjdk_version"
        ( sed -i.bak 's!%%BASE_TAG%%!'"$openjdk_version"'!g' "$dir/Dockerfile-$openjdk_version"
          sed -i.bak 's!%%MAINTAINER%%!'"$maintainer"'!g' "$dir/Dockerfile-$openjdk_version"
          sed -i.bak 's!%%BUILD_TOOL_VERSION%%!'"$bt_version"'!g' "$dir/Dockerfile-$openjdk_version"
          if [ "$bv" = "alpine" ]; then
              sed -i.bak 's/^%%ALPINE%% //g' "$dir/Dockerfile-$openjdk_version"
          else
              sed -i.bak '/^%%ALPINE%%/d' "$dir/Dockerfile-$openjdk_version"
              sed -i.bak '/^$/N;/^\n$/D' "$dir/Dockerfile-$openjdk_version"
          fi
        )
        find . -name "*.bak" -type f -delete
  done
done
