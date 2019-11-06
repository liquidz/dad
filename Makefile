.PHONY: native-image jar_test bin_test circleci test clean

PLATFORM = $(shell uname -s)
ifeq ($(PLATFORM), Darwin)
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

target/daddy.jar:
	lein uberjar
dad.linux-amd64:
	./script/build

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
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
		--no-server \
		$(GRAAL_EXTRA_OPTION) \
		"-J-Xmx3g"

native-image: clean dad

jar_test: target/daddy.jar
	./script/jar_test

bin_test: dad.linux-amd64
	./script/bin_test

circleci:
	circleci local execute --job debian

test:
	lein test

clean:
	lein clean
	\rm -f dad dad.linux-amd64
