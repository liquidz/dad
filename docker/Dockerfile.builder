FROM clojure:openjdk-8-lein
WORKDIR /tmp

ENV GRAALVM_HOME /tmp/graalvm-ce-19.2.0
RUN apt update \
  && apt install -y curl make gcc zlib1g-dev \
  && curl -O -sL https://github.com/oracle/graal/releases/download/vm-19.2.0/graalvm-ce-linux-amd64-19.2.0.tar.gz \
  && tar xzf graalvm-ce-linux-amd64-19.2.0.tar.gz \
  && ${GRAALVM_HOME}/bin/gu install native-image \
  && apt-get clean \
  && rm -rf /var/lib/apt/lists/* \
  && mkdir /src

WORKDIR /src
