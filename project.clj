(defproject trattoria
  #=(clojure.string/trim #=(slurp "resources/version.txt"))
  :description "Careless configuration management tool"
  :url "https://github.com/liquidz/trattoria"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [borkdude/sci "0.0.9"]
                 [org.clojure/tools.cli "0.4.2"]]

  :main ^{:skip-aot true} trattoria.core
  :profiles
  {:uberjar {:aot [trattoria.core]
             :prep-tasks ["compile"]
             :uberjar-name "trattoria.jar"}}
  )
