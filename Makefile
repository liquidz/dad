.PHONY: native-image circleci clean

PLATFORM = $(shell uname -s)
ifeq ($(PLATFORM), Darwin)
	GRAAL_EXTRA_OPTION :=
else
	GRAAL_EXTRA_OPTION := "--static"
endif

target/trattoria.jar:
	lein uberjar

trattoria: target/trattoria.jar
	$(GRAALVM_HOME)/bin/native-image \
		-jar target/trattoria.jar \
		-H:Name=trattoria \
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

native-image: trattoria

circleci:
	circleci local execute --job debian

clean:
	lein clean
	\rm -f trattoria
