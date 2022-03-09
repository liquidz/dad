(defproject dad
  #=(clojure.string/trim #=(slurp "resources/version.txt"))
  :description "Small configuration management tool for Clojure"
  :url "https://github.com/liquidz/dad"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [org.clojure/tools.cli "1.0.206"]

                 [aero "1.1.6"]
                 [babashka/babashka.nrepl "0.0.6"]
                 [org.babashka/sci "0.3.2"]
                 [camel-snake-kebab "0.4.2"]
                 [metosin/malli "0.8.4"]
                 ;; pod
                 [nrepl/bencode "1.1.0"]]

  :main ^{:skip-aot true} dad.core
  :profiles
  {:dev {:source-patsh ["dev"]
         :resource-paths ["test/resources"]
         :global-vars {*warn-on-reflection* true}}
   :outdated {:dependencies [[com.github.liquidz/antq "RELEASE"]]}
   :uberjar {:aot :all
             :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
             :prep-tasks ["compile"]
             :uberjar-name "dad.jar"}})
