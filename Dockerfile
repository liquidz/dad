FROM clojure:openjdk-11-lein
WORKDIR /tmp

ENV GRAALVM_HOME /tmp/graalvm-ce-java11-19.3.0

RUN apt update \
  && apt install -y curl make gcc zlib1g-dev \
  && curl -O -sL https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-19.3.0/graalvm-ce-java11-linux-amd64-19.3.0.tar.gz \
  && tar xzf graalvm-ce-java11-linux-amd64-19.3.0.tar.gz \
  && ${GRAALVM_HOME}/bin/gu install native-image \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir /src

WORKDIR /src
