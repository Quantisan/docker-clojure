FROM clojure:alpine
MAINTAINER Wes Morgan <wesmorgan@icloud.com>

WORKDIR /usr/src/app

ONBUILD COPY project.clj /usr/src/app/
ONBUILD RUN lein deps
ONBUILD COPY . /usr/src/app

CMD ["lein", "run"]
