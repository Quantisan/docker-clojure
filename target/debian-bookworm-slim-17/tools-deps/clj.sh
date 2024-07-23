#!/bin/sh

# This script acts as a replacement for "clj" script shipped with Clojure CLI
# (tools.deps) installation. Original "clj" uses rlwrap to wrap "clojure" script
# and provide line editing abilities. This script uses rlfe instead of rlwrap
# for the same purpose. If rlfe is not installed, run regular "clojure" script.

if type rlfe; then
    exec rlfe clojure "$@"
else
    exec clojure "$@"
fi
