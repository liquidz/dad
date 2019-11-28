.PHONY: native-image test release clean
.PHONY: jar_test local_jar_test bin_test example_jar_test example_bin_test

PLATFORM = $(shell uname -s)
ifeq ($(PLATFORM), Darwin)
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

target/daddy.jar:
	env LEIN_SNAPSHOTS_IN_RELEASE=1 lein uberjar
dad.linux-amd64:
	./script/linux-build

dad: target/daddy.jar
	$(GRAALVM_HOME)/bin/native-image \
		-jar target/daddy.jar \
		-H:Name=dad \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		"-H:IncludeResources=command.edn" \
		"-H:IncludeResources=schema.edn" \
		"-H:IncludeResources=config.edn" \
		"-H:IncludeResources=version.txt" \
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

jar_test: target/daddy.jar
	env TARGET=target/daddy.jar IMAGE_NAME=clojure:openjdk-11-lein ./script/test

local_jar_test: target/daddy.jar
	\cp -pf target/daddy.jar test/resources/test_task
	(cd test/resources/test_task && bash run.sh)
	\rm -f test/resources/test_task/daddy.jar

bin_test: dad.linux-amd64
	env TARGET=dad.linux-amd64 IMAGE_NAME=ubuntu:latest ./script/test

example_jar_test: target/daddy.jar
	env TARGET=target/daddy.jar IMAGE_NAME=clojure:openjdk-11-lein ./script/example

example_bin_test: dad.linux-amd64
	env TARGET=dad.linux-amd64 IMAGE_NAME=ubuntu:latest ./script/example

test:
	lein test

release:
	./script/release

clean:
	lein clean
	\rm -f dad dad.linux-amd64
