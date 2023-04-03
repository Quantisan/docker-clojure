# docker-clojure

This is the repository for the [official Docker image for Clojure](https://registry.hub.docker.com/_/clojure/).
It is automatically pulled and built by Stackbrew into the Docker registry.
This image runs on OpenJDK 8, 11, 17, and more recent releases and includes
[Leiningen](http://leiningen.org) or [tools-deps](https://clojure.org/reference/deps_and_cli)
(see below for tags and building instructions).

## Leiningen vs. tools-deps

The version tags on these images look like `(temurin-major-version-)lein-N.N.N(-distro)`,
or `(temurin-major-version-)tools-deps(-distro)`. These refer to which version
of leiningen or tools-deps is packaged in the image (because they can then install
and use any version of Clojure at runtime). The `lein` (or `lein-bullseye-slim`,
`temurin-17-lein`, etc.) images will always have a recent version of leiningen
installed. If you want to use tools-deps, specify either `clojure:tools-deps`,
`clojure:tools-deps-bullseye-slim` or other similar variants.

### boot

Prior to JDK 20 we provided [boot](https://boot-clj.github.io) variants as well. 
Boot hasn't had a release since 2019, and it is breaking in more and more image
variants. Boot variants are now deprecated and no new boot images will be
provided for JDK 20+, it will no longer be installed in the `latest` image, and
no alpine-based images will be provided for any JDK version (due to breakage).
As long as the image builds don't break, we will continue providing boot images
for non-alpine distros for JDK 17 and lower until those versions are EOL'd.

### Note about the latest tag

As of 2020-3-20 the `clojure:latest` (also `clojure` because `latest` is the
default) now has leiningen and tools-deps installed.

Previously this tag only had leiningen installed. Installing tools-deps too is
helpful for quick start examples, newcomers, etc. as leiningen is by no means
the de facto standard build tool these days. The downside is that the image is
larger. But for the `latest` tag it's a good trade-off because for anything real
we have always recommended using more specific tags. No other tags are affected
by this change.

## JDK versions

Java follows a release cadence of every 6 months with an LTS (long-term support)
release every 3 years. As of 2019-9-25, our images will default to the latest
LTS release of OpenJDK (currently 17). But we also now provide the ability to
specify which version of Java you'd like via Docker tags:

JDK 1.8 tools-deps image: `clojure:temurin-8-tools-deps`
JDK 11 variant of the tools-deps image: `clojure:temurin-11-tools-deps` or `clojure:temurin-11`
JDK 17 variant of the tools-deps image: `clojure:tools-deps` or `clojure:temurin-17` or `clojure:temurin-17-tools-deps`
JDK 17 with boot 2.8.3: `clojure:temurin-17-boot-2.8.3`
JDK 20 with the latest version of lein: `clojure:temurin-20-lein`

## Linux distro

The upstream eclipse-temurin images are built on different versions of Ubuntu,
so we have exposed those in our Docker tags as well. However, Ubuntu is not the
best distro for Docker images in many cases. In particular, their insistence on
replacing traditional dpkg packages with snaps renders them essentially
unusable inside containers because they need a daemon running to even install.
To mitigate this we provide Debian-based images that copy the Java bits from
the eclipse-temurin images (which is what the Temurin folks recommend when you
want a different base image than what they provide). It is recommended to use
the Debian variants unless you have a need to use Ubuntu or stick closer to the
official upstream images.

As of 2022-9-29 the default distro is Ubuntu jammy in order to maintain
backwards compatibility. But you should not interpret this default as a
recommendation. Use Debian bullseye or bullseye-slim variants unless you have
a good, specific reason not to. There are fewer dead ends that way.

You can specify which distro & version you'd like by appending it to the end of
your Docker tag as in the following examples (but note that not every
combination is provided upstream and thus likewise for us):

Java 8 leiningen on Debian bullseye-slim: `clojure:temurin-8-lein-bullseye-slim`
Java 11 leiningen on Debian bullseye: `clojure:temurin-11-lein-bullseye`
Java 17 tools-deps on Ubuntu focal: `clojure:tools-deps` or `clojure:temurin-17` or `clojure:temurin-17-tools-deps` or `clojure:temurin-17-tools-deps-focal`
Java 17 tools-deps on Debian bullseye-slim: `clojure:bullseye-slim` or `clojure:tools-deps-bullseye-slim` or `clojure:temurin-17-bullseye-slim` or `clojure:temurin-17-tools-deps-bullseye-slim`

### Alpine Linux

Sometimes there are upstream eclipse-temurin images based on Alpine Linux.

As of 2022-9-29 these are available for the linux/amd64 architecture only.

Some example tags:

Java 17 leiningen on Alpine: `clojure:temurin-17-alpine` `clojure:temurin-17-lein-alpine`
Java 20 tools-deps on Alpine: `clojure:temurin-20-tools-deps-alpine` or `clojure:temurin-20-alpine`

### `clojure:slim-buster` / `clojure:slim-bullseye`

These images are based on the Debian buster distribution but have fewer
packages installed and are thus a bit smaller than the `buster` or `bullseye`
images. Their use is recommended.

Note that as of 2022-9-29 there are no `slim-focal` images published by the
eclipse-temurin maintainers, so the slim option there is the `alpine` variant.

## Examples

### Interactive Shell

Run an interactive shell from this image.

```
docker run -ti clojure bash
```

Then within the shell, create a new Leiningen project and start a Clojure REPL.

```
lein new hello-world
cd hello-world
lein repl
```

## Builds

The Dockerfiles are generated by the `docker-clojure` Clojure app in this repo.

You'll need a recent version of [Babashka](https://babashka.org/) installed to
run the builds.

Take a look at the `:tasks` key in the `bb.edn` file in the root of this repo.
Each of these can be invoked via `bb run [task]` at the command line.

For example, to build all images locally, run `bb run build-images`.

### buildx

Note that you'll need to enable the new `buildx` feature and set as the default
builder in the Docker daemon you're using to build the images. The build script
uses some flags that require it.

You'll also need to create a builder container with `docker buildx create --use`
in order to build images for all supported platforms (currently linux/amd64 and
linux/arm64).

[Read the docs here](https://docs.docker.com/buildx/working-with-buildx/) for more info.

## Tests

The `docker-clojure` build tool has a test suite that can be run via the
`bb run test` script. 
