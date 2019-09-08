(ns trattoria.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [trattoria.reader :as t.reader]
            [trattoria.runner :as t.runner]
            [trattoria.runner.directory]
            [trattoria.runner.execute]
            [trattoria.runner.git]
            [trattoria.runner.file]
            [trattoria.runner.package]
            [trattoria.runner.template]
            ))

(def cli-options
  [["-d" "--dryrun"]
   ["-h" "--help"]])

(defn -main [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [help]} options]
    (cond
      errors (doseq [e errors] (println e))
      help (println (str "Usage:\n" summary))

      :else
      (try
        (some-> arguments first io/file slurp
                t.reader/read-tasks
                t.runner/run-tasks
                )
        (catch Exception ex
          ;(.printStackTrace ex)
          (println (.getMessage ex) (ex-data ex))
          (System/exit 1))))
    (System/exit 0)))
