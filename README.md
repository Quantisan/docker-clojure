# docker-clojure

This is the repository for the [Docker Official Image for Clojure](https://registry.hub.docker.com/_/clojure/).
[Docker Official Images](https://docs.docker.com/docker-hub/image-library/trusted-content/#docker-official-images)
are a curated set of Docker repositories hosted on Docker Hub.

It is automatically pulled and built by Stackbrew into the Docker registry.
This image runs on OpenJDK 8, 11, 17, and more recent releases and includes
[Leiningen](http://leiningen.org) or [tools-deps](https://clojure.org/reference/deps_and_cli)
(see below for tags and building instructions).

## Leiningen vs. tools-deps

The version tags on these images look like `(temurin-major-version-)lein-N.N.N(-distro)`,
or `(temurin-major-version-)tools-deps-N.N.N.N(-distro)`. These refer to which version
of leiningen or tools-deps is packaged in the image (because they can then install
and use any version of Clojure at runtime). The `lein` (or `lein-bullseye-slim`,
`temurin-17-lein`, etc.) images will always have a recent version of leiningen
installed. If you want to use tools-deps, specify either `clojure:tools-deps`,
`clojure:tools-deps-bullseye-slim` or other similar variants.

### boot

As of 5/2024, new [boot](https://boot-clj.github.io) images are no longer
provided. The existing boot images will remain in the registry.

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
release every 2-3 years. As of 2019-9-25, our images will default to the latest
LTS release of OpenJDK (currently 21). But we also now provide the ability to
specify which version of Java you'd like via Docker tags:

JDK 1.8 tools-deps image: `clojure:temurin-8-tools-deps`  
JDK 11 variant of the tools-deps image: `clojure:temurin-11-tools-deps` or `clojure:temurin-11`  
JDK 17 with lein 2.11.2: `clojure:temurin-17-lein-2.11.2`  
JDK 20 with the latest version of lein: `clojure:temurin-20-lein`  
JDK 21 variant of the tools-deps image: `clojure:tools-deps` or `clojure:temurin-21` or `clojure:temurin-21-tools-deps`  
JDK 25 variant of the tools-deps image: `clojure:temurin-25` or `clojure:temurin-25-tools-deps`

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

As of 2023-10-12 the default distro is Debian bookworm (12) in JDK 21+ images
and Ubuntu jammy (22.04) in JDK <=20 images in order to maintain backwards
compatibility. But we recommend you use Debian variants on any JDK version
unless you have a good, specific reason not to. There are fewer dead ends that
way.

### debian-slim

For a lighter-weight image, try the Debian `-slim` variants. You can just put
`bookworm-slim` (for Debian 12) or `bullseye-slim` (for Debian 11) in the distro
slot of your Docker tag to get those.

### Examples

You can specify which distro & version you'd like by appending it to the end of
your Docker tag as in the following examples (but note that not every
combination is provided upstream and thus likewise for us):

Java 8 leiningen on Debian bullseye-slim: `clojure:temurin-8-lein-bullseye-slim`  
Java 11 leiningen on Debian bullseye: `clojure:temurin-11-lein-bullseye`  
Java 17 tools-deps on Ubuntu noble: `clojure:tools-deps` or `clojure:temurin-17` or `clojure:temurin-17-tools-deps` or `clojure:temurin-17-tools-deps-noble`  
Java 17 tools-deps on Debian bullseye-slim: `clojure:bullseye-slim` or `clojure:tools-deps-bullseye-slim` or `clojure:temurin-17-bullseye-slim` or `clojure:temurin-17-tools-deps-bullseye-slim`  
Java 21 tools-deps on Debian bookworm: `clojure:tools-deps` or `clojure:temurin-21-tools-deps` or `clojure:temurin-21-bookworm`  
Java 25 leiningen on Debian bookworm: `clojure:temurin-25-lein-bookworm`

### Alpine Linux

Alpine Linux is another light-weight option that is a popular base image in the
Docker community. When an upstream eclipse-temurin image is available for a
given Java release and architecture, we will provide a clojure image based on it
if we can.

As of 2025-04-28 these are available for both amd64 and arm64 architectures, but
only for Java 21+. For Java versions below 21, only amd64 is available.

Some example tags:

Java 17 leiningen on Alpine: `clojure:temurin-17-alpine` `clojure:temurin-17-lein-alpine`  
Java 21 tools-deps on Alpine: `clojure:temurin-21-tools-deps-alpine` or `clojure:temurin-21-alpine`

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
