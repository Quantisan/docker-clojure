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

declare -A maintainers=(
  [debian/lein]=$PAUL
  [debian/boot]=$WES
  [alpine]=$WES
)


# Dockerfile generator

variants=( "$@" )
if [ ${#variants[@]} -eq 0 ]; then
  variants=( */*/ )
fi
variants=( "${variants[@]%/}" )

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
  openjdk_version=$OPENJDK_VERSION
  dir="$variant"
  base_variant=${variant%/*}
  build_tool=${variant#*/}
  echo "Generating Dockerfile for $dir"
  [ -d "$dir" ] || continue
  template="Dockerfile-$build_tool.template"
  echo "Using template $template"
  if [ "$base_variant" != "debian" ]; then
    openjdk_version="${openjdk_version}-${base_variant}"
  fi
  maintainer=${maintainers[$variant]:-${maintainers[$base_variant]}}
  { generated_warning; cat "$template"; } > "$dir/Dockerfile"
  ( set -x
    sed -i '' 's!%%BASE_TAG%%!'"$openjdk_version"'!g' "$dir/Dockerfile"
    sed -i '' 's!%%MAINTAINER%%!'"$maintainer"'!g' "$dir/Dockerfile"
    if [ "$base_variant" = "alpine" ]; then
      sed -i '' 's/^%%ALPINE%% //g' "$dir/Dockerfile"
    else
      sed -i '' '/^%%ALPINE%%/d' "$dir/Dockerfile"
      sed -i '' '/^$/N;/^\n$/D' "$dir/Dockerfile"
    fi
    if [ "$base_variant" = "debian" ]; then
      sed -i '' 's/^%%DEBIAN%% //g' "$dir/Dockerfile"
    else
      sed -i '' '/^%%DEBIAN%%/d' "$dir/Dockerfile"
      sed -i '' '/^$/N;/^\n$/D' "$dir/Dockerfile"
    fi
  )
done
