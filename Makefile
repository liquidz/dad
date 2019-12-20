.PHONY: native-image test release clean
.PHONY: jar_test local_jar_test bin_test example_jar_test example_bin_test

PLATFORM = $(shell uname -s)
ifeq ($(PLATFORM), Darwin)
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

prepare:
	\cat doc/*.adoc > resources/docs.adoc

target/dad.jar: prepare
	env LEIN_SNAPSHOTS_IN_RELEASE=1 lein uberjar
dad.linux-amd64:
	./script/linux-build

dad: target/dad.jar
	$(GRAALVM_HOME)/bin/native-image \
		-jar target/dad.jar \
		-H:Name=dad \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		"-H:IncludeResources=command.edn" \
		"-H:IncludeResources=schema.edn" \
		"-H:IncludeResources=config.edn" \
		"-H:IncludeResources=version.txt" \
		"-H:IncludeResources=docs.adoc" \
		--initialize-at-build-time  \
		--report-unsupported-elements-at-runtime \
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
		--no-server \
		$(GRAAL_EXTRA_OPTION) \
		"-J-Xmx3g"

native-image: clean dad
	lein clean

jar_test: target/dad.jar
	env TARGET=target/dad.jar IMAGE_NAME=clojure:openjdk-11-lein ./script/test

local_jar_test: target/dad.jar
	\cp -pf target/dad.jar test/resources/test_task
	(cd test/resources/test_task && bash run.sh)
	\rm -f test/resources/test_task/dad.jar

bin_test: dad.linux-amd64
	env TARGET=dad.linux-amd64 IMAGE_NAME=ubuntu:latest ./script/test

example_jar_test: target/dad.jar
	env TARGET=target/dad.jar IMAGE_NAME=clojure:openjdk-11-lein ./script/example

example_bin_test: dad.linux-amd64
	env TARGET=dad.linux-amd64 IMAGE_NAME=ubuntu:latest ./script/example

test: prepare
	lein test

release:
	./script/release

clean:
	lein clean
	\rm -f dad dad.linux-amd64
	\rm -f resources/docs.adoc
