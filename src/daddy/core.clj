(ns daddy.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [daddy.config :as d.config]
            [daddy.logger :as d.log]
            [daddy.os :as d.os]
            [daddy.reader :as d.reader]
            [daddy.runner :as d.runner]))

(def ^:private cli-options
  [["-s" "--silent"]
   [nil "--debug"]
   ["-n" "--dry-run"]
   [nil "--no-color"]
   ["-h" "--help"]
   ["-v" "--version"]])

(defn- print-version []
  (let [ver (-> "version.txt" io/resource slurp str/trim)]
    (println (str "daddy ver " ver))
    (println (str "* Detected OS: " (name (d.os/os-type))))))

(defn- usage [summary]
  (print-version)
  (println (str "* Usage:\n" summary)))

(defn -main [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [debug dry-run no-color help silent version]} options
        config (d.config/read-config)
        log-level (cond
                    silent :silent
                    debug :debug
                    :else :info)
        runner-fn (if dry-run
                    d.runner/dry-run-tasks
                    d.runner/run-tasks)]
    (cond
      errors (doseq [e errors] (println e))
      help (usage summary)
      version (print-version)
      :else
      (binding [d.log/*level* log-level
                d.log/*color* (not no-color)]
        (try
          (some->> arguments
                   (map slurp)
                   (str/join "\n")
                   (d.reader/read-tasks config)
                   (runner-fn config))
          (catch Exception ex
            (d.log/error (.getMessage ex) (ex-data ex))
            (System/exit 1)))))
    (System/exit 0)))
