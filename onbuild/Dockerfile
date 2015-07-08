FROM clojure:latest
MAINTAINER Kirillov Alexandr <kirillov@evrone.ru>

WORKDIR /usr/src/app

ONBUILD COPY project.clj /usr/src/app/
ONBUILD RUN lein deps
ONBUILD COPY . /usr/src/app

CMD ["lein", "run"]
