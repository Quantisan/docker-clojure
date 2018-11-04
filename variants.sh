# intended for sourcing by other scripts, not direct execution

declare -A versions=(
  [lein]=2.8.1
  [boot]=2.8.1
  [tools-deps]=1.9.0.397
)

variants=( "$@" )
if [ ${#variants[@]} -eq 0 ]; then
  variants=( */*/ )
fi
variants=( "${variants[@]%/}" )

function base_variant {
  local variant=$1
  echo ${variant%/*}
}

function build_tool {
  local variant=$1
  echo ${variant#*/}
}

function build_tool_version {
  local build_tool=$1
  echo ${versions[$build_tool]}
}

function image_tag {
  local variant=$1
  local bv=$(base_variant $variant)
  local bt=$(build_tool $variant)
  local bt_version=$(build_tool_version $bt)
  local tag="${bt}-${bt_version}"
  if [[ $bv != "debian" ]]; then
    tag="${tag}-${bv}"
  fi
  echo "$tag"
}

function image_name {
  local variant=$1
  local tag=$(image_tag $variant)
  echo "clojure:$tag"
}
