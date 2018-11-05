#!/usr/bin/env bash

set -e

clojure -m docker-clojure.core dockerfiles
