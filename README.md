# docker-clojure

This is the repository for the [official Docker image for Clojure](https://registry.hub.docker.com/_/clojure/).
It is automatically pulled and built by Stackbrew into the Docker registry.
This image runs on OpenJDK 8 and includes Leiningen.

## Examples

### Interactive Shell

Run an interactive shell from this image.

```
docker run -i -t clojure /bin/bash
```

Then within the shell, create a new Leiningen project and start a Clojure REPL.

```
lein new hello-world
cd hello-world
lein repl
```

### Isolated Development with [Fig](http://www.fig.sh/)

See [this repository for instruction and example](https://github.com/Quantisan/clojure-getting-started).
