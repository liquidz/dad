(defproject trattoria
  #=(clojure.string/trim #=(slurp "resources/version.txt"))
  :description "FIXME"
  :url "https://github.com/liquidz/trattoria"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
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
