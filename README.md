# docker-clojure

This is the repository for the [official Docker image for Clojure](https://registry.hub.docker.com/_/clojure/).
It is automatically pulled and built by Stackbrew into the Docker registry.
This image runs on OpenJDK 8 and includes [Leiningen](http://leiningen.org), [boot](http://boot-clj.com) or [tools-deps](https://clojure.org/reference/deps_and_cli) (see below for tags and building instructions).

## Leiningen vs. boot vs. tools-deps

The version tags on these images look like `lein-N.N.N(-alpine)`, `boot-N.N.N(-alpine)` and `tools-deps(-alpine)`. These refer to which version of leiningen, boot or tools-deps is packaged in the image (because they can then install and use any version of Clojure at runtime). The default `latest` (or `lein`, `lein-alpine`) images will always have a recent version of leiningen installed. If you want boot, specify either `clojure:boot` / `clojure:boot-alpine` or `clojure:boot-N.N.N` / `clojure:boot-N.N.N-alpine`. (where `N.N.N` is the version of boot you want installed). If you want to use tools-deps, specify either `clojure:tools-deps` or `clojure:tools-deps-alpine`.

## Alpine vs. Debian

In an effort to minimize the size of Docker images, the Docker community is migrating from traditional Linux distributions (like Debian) to the Alpine Linux distribution. It is a very minimal distribution specifically designed for container applications.

The traditional Debian-based Clojure images are still provided under the original tags, but you are encouraged to try running your applications in the new `alpine` variants. Since Clojure runs on the JVM anyway, chances are everything will just work and you'll save yourself some time and bandwidth.

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

### On-build triggers

#### Build the image

```
docker build -t clojure:onbuild debian/lein/onbuild
```

#### Create a `Dockerfile` in your Clojure app project

```
FROM clojure:onbuild
CMD ["lein", "run"]
```

Put this file in the root of your app.

This image includes multiple `ONBUILD` triggers which should be all you need to bootstrap most applications. The build will `COPY . /usr/src/app` and `RUN lein deps`

You can then build and run the CLojure image:

```
docker build -t my-clojure-app .
docker run -it --rm --name running-clojure-app my-clojure-app
```

**NOTE:** The `onbuild` image is designed to get you up and running quickly in a development environment. You probably do not want to deploy your applications to production servers using the `onbuild` image. Compiling an uberjar and running it via `java -jar` or using `lein run trampoline` would be much more memory-efficient.

## Builds

Except for the `onbuild` variants, the Dockerfiles are generated from the `Dockerfile-lein.template` and `Dockerfile-boot.template` files by the `update.sh` script.

If you want to modify them (for example, to build in a different version of lein or boot), edit the template(s) and then run `./update.sh`. Then the example `docker build ...` commands below will do what you want.

Note that the `update.sh` script requires bash version 4+ to run.

If you then want to build all of the non-onbuild images, run the
`build-images.sh` script. This will tag them as shown in the
examples below. So if you want to push any of them to your own
Docker repo, run something like this after building:
`docker tag clojure:lein-2.8.1 my-repo/clojure:lein-2.8.1`.

### Build examples

#### Debian-based leiningen

```
docker build -t clojure:lein-2.8.1 debian/lein
```

#### Debian-based boot

```
docker build -t clojure:boot-2.7.2 debian/boot
```

#### Debian-based tools-deps

```
docker build -t clojure:tools-deps debian/tools-deps
```

#### Alpine-based leiningen

```
docker build -t clojure:lein-2.8.1-alpine alpine/lein
```

#### Alpine-based boot

```
docker build -t clojure:boot-2.7.2-alpine alpine/boot
```

#### Alpine-based tools-deps

```
docker build -t clojure:tools-deps-alpine alpine/tools-deps
```
