set -e

commands=$(java -cp "/usr/share/java/leiningen-$LEIN_VERSION-standalone.jar" clojure.main -e "$1")

stub=$(mktemp -d)
printf '%s\n' '#!/bin/sh' 'echo "lein $1"' > "$stub/lein"
chmod +x "$stub/lein"

# Built-ins must route to lein without the fallback warning, even when an
# executable on the image's PATH shares their name.
status=0
while IFS= read -r command; do
  routed=$(PATH="$stub:$PATH" /usr/local/bin/entrypoint "$command" 2>&1 </dev/null) || true
  if [ "$routed" != "lein $command" ]; then
    echo "entrypoint does not pass $command to lein: $routed" >&2
    status=1
  fi
done <<EOF
$commands
EOF

exit $status
