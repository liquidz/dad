.PHONY: native-image clean

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
		--initialize-at-build-time  \
		-H:Log=registerResource: \
		--verbose \
		--no-fallback \
		--no-server \
		--static \
		"-J-Xmx3g"

native-image: target/trattoria

clean:
	lein clean
	\rm -f trattoria
