FROM clojure:openjdk-11-tools-deps
WORKDIR /tmp

RUN apt update \
  && apt install -y curl make sudo \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir /src

COPY Makefile /tmp/Makefile

RUN make graalvm

WORKDIR /src
