.PHONY: native-image

target/trattoria.jar:
	lein uberjar

target/trattoria: target/trattoria.jar
	$(GRAALVM_HOME)/bin/native-image \
		-jar target/trattoria.jar \
		-H:Name=trattoria \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		"-H:IncludeResources=command/.*" \
		--initialize-at-build-time  \
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
		--no-server \
		"-J-Xmx3g"

native-image: target/trattoria
