set -e

commands=$(java -cp /usr/share/java/leiningen-*-standalone.jar clojure.main -e "$1")

stub=$(mktemp -d)
printf '%s\n' '#!/bin/sh' 'echo "lein $1"' > "$stub/lein"
chmod +x "$stub/lein"

status=0
while IFS= read -r command; do
  routed=$(PATH="$stub:/usr/bin:/bin" /usr/local/bin/entrypoint "$command" 2>/dev/null) || true
  if [ "$routed" != "lein $command" ]; then
    echo "entrypoint does not pass $command to lein" >&2
    status=1
  fi
done <<EOF
$commands
EOF

exit $status
