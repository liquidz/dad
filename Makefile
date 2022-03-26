# GraalVM {{{
PLATFORM := $(shell uname -s | tr '[:upper:]' '[:lower:]')
GRAAL_ROOT ?= /tmp/.graalvm
GRAAL_VERSION ?= 22.0.0.2
GRAAL_HOME ?= $(GRAAL_ROOT)/graalvm-ce-java11-$(GRAAL_VERSION)
GRAAL_ARCHIVE := graalvm-ce-java11-$(PLATFORM)-amd64-$(GRAAL_VERSION).tar.gz

ifeq ($(PLATFORM),darwin)
	GRAAL_HOME := $(GRAAL_HOME)/Contents/Home
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

$(GRAAL_ROOT)/fetch/$(GRAAL_ARCHIVE):
	@mkdir -p $(GRAAL_ROOT)/fetch
	curl --location --output $@ https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$(GRAAL_VERSION)/$(GRAAL_ARCHIVE)

$(GRAAL_HOME): $(GRAAL_ROOT)/fetch/$(GRAAL_ARCHIVE)
	tar -xz -C $(GRAAL_ROOT) -f $<

$(GRAAL_HOME)/bin/native-image: $(GRAAL_HOME)
	$(GRAAL_HOME)/bin/gu install native-image

.PHONY: graalvm
graalvm: $(GRAAL_HOME)/bin/native-image
# }}}

target/dad.jar:
	clojure -T:build uberjar

.PHONY: uberjar
uberjar: target/dad.jar

dad.linux-amd64:
	./script/linux-build

.PHONY: dad
dad: graalvm uberjar
	$(GRAAL_HOME)/bin/native-image \
		-jar target/dad.jar \
		-H:Name=dad \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		"-H:IncludeResources=command.edn" \
		"-H:IncludeResources=config.edn" \
		"-H:IncludeResources=version.txt" \
		--report-unsupported-elements-at-runtime \
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
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
	env TARGET=target/dad.jar IMAGE_NAME=openjdk:11 ./script/example

.PHONY: example_bin_test
example_bin_test: dad.linux-amd64
	env TARGET=dad.linux-amd64 IMAGE_NAME=ubuntu:latest ./script/example

.PHONY: generate_docs
generate_docs:
	clojure -M:generate-docs

.PHONY: test
test:
	clojure -M:dev:test

.PHONY: release
release:
	./script/release

.PHONY: outdated
outdated:
	clojure -M:outdated --upgrade

.PHONY: clean
clean:
	\rm -rf target .cpcache
	\rm -f dad dad.linux-amd64

# vim:fdl=0:fdm=marker:
