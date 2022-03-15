PLATFORM = $(shell uname -s)
ifeq ($(PLATFORM), Darwin)
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

.PHONY: prepare
prepare:
	\cat doc/*.adoc > resources/docs.adoc

target/dad.jar: prepare
	clojure -T:build uberjar
dad.linux-amd64:
	./script/linux-build

.PHONY: dad
dad: uberjar
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

.PHONY: native-image
native-image: clean dad

.PHONY: jar_test
jar_test: target/dad.jar
	env TARGET=target/dad.jar IMAGE_NAME=clojure:openjdk-11 ./script/test

.PHONY: local_jar_test
local_jar_test: target/dad.jar
	\cp -pf target/dad.jar test/resources/test_task
	(cd test/resources/test_task && bash run.sh)
	\rm -f test/resources/test_task/dad.jar

.PHONY: bin_test
bin_test: dad.linux-amd64
	env TARGET=dad.linux-amd64 IMAGE_NAME=ubuntu:latest ./script/test

.PHONY: example_jar_test
example_jar_test: target/dad.jar
	env TARGET=target/dad.jar IMAGE_NAME=clojure:openjdk-11 ./script/example

.PHONY: example_bin_test
example_bin_test: dad.linux-amd64
	env TARGET=dad.linux-amd64 IMAGE_NAME=ubuntu:latest ./script/example

.PHONY: test
test: prepare
	clojure -M:dev:test

# release:
# 	./script/release

.PHONY: outdated
outdated:
	clojure -M:outdated

.PHONY: clean
clean:
	\rm -rf target .cpcache
	\rm -f dad dad.linux-amd64
	\rm -f resources/docs.adoc
