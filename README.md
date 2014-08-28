# docker-clojure

This is the repository for the [official Docker image for Clojure](https://registry.hub.docker.com/_/clojure/). It is automatically
pulled and built by Stackbrew into the Docker registry. This image runs on OpenJDK 7 and
includes Leiningen.

Note that for production, simply use the `java` image and run compiled Clojure
code as regular JAR so that Leiningen doesn't need to be running.

## Example

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