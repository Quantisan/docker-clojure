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

### `clojure:onbuild`

This image makes building derivative images easier. For most use cases, creating a Dockerfile in the base of your project directory with the line `FROM clojure:onbuild` will be enough to create a stand-alone image for your project.

### `clojure:alpine` & `clojure:alpine-onbuild`

These images are based on the minimalist Alpine Linux distribution and are much smaller than the default (Debian-based) images. If your app can run in them, they will be much faster to transfer and take up much less space.

#### Pull or build this image

```
docker build --rm -t clojure:onbuild onbuild
```

#### Create a `Dockerfile` in your Clojure app project

```
FROM clojure:onbuild
```

Put this file in the root of your app.

This image includes multiple `ONBUILD` triggers which should be all you need to bootstrap most applications. The build will `COPY . /usr/src/app` and `RUN lein deps`

You can then build and run the CLojure image:

```
docker build -t my-clojure-app .
docker run -it --rm --name running-clojure-app my-clojure-app
```

### Isolated Development with [Fig](http://www.fig.sh/)

See [this repository for instruction and example](https://github.com/Quantisan/clojure-getting-started).

### ClojureScript Development with [Figwheel](https://github.com/bhauman/lein-figwheel) ###

Run Figwheel to automatically load your ClojureScript changes into your running browser window.

```
docker run --rm -it \
  -w /w -v "$PWD":/w \
  -v "$HOME"/.m2:/root/.m2 \
  -p 3449:3449 \
  clojure \
  rlfe lein figwheel
```

Notes:
- Mounts $HOME/.m2 for caching Maven jars.
- Exposes port 3449 (the default port for Figwheel) so the client code running in your browser can connect to it.
- Wraps Figwheel's REPL in `rlfe` so command history and standard key combinations are available
- If you're using a virtual machine, be sure to set Figwheel's `:websocket-url` option in your project.clj to something that makes sense (e.g., `ws://192.168.99.100:3449/figwheel-ws`).
