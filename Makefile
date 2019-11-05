.PHONY: native-image circleci test clean

PLATFORM = $(shell uname -s)
ifeq ($(PLATFORM), Darwin)
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

target/daddy.jar:
	lein uberjar

dad: target/daddy.jar
	$(GRAALVM_HOME)/bin/native-image \
		-jar target/daddy.jar \
		-H:Name=dad \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		"-H:IncludeResources=command/.*" \
		"-H:IncludeResources=version.txt" \
		--initialize-at-build-time  \
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
		--no-server \
		$(GRAAL_EXTRA_OPTION) \
		"-J-Xmx3g"

native-image: clean dad

circleci:
	circleci local execute --job debian

test:
	lein test

clean:
	lein clean
	\rm -f dad
