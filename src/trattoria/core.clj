(ns trattoria.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [trattoria.os :as t.os]
            [trattoria.reader :as t.reader]
            [trattoria.runner :as t.runner]
            [trattoria.runner.directory]
            [trattoria.runner.execute]
            [trattoria.runner.file]
            [trattoria.runner.git]
            [trattoria.runner.link]
            [trattoria.runner.package]
            [trattoria.runner.template]
            [clojure.string :as str]))

(def cli-options
  [[nil "--test"]
   ["-d" "--dryrun"]
   ["-h" "--help"]
   ["-v" "--version"]])

(defn- print-version []
  (let [ver (-> "version.txt" io/resource slurp str/trim)]
    (println (str "trattoria ver " ver))
    (println (str "* Detected OS: " (name t.os/os-type)))))

(defn -main [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [help version]} options]
    (cond
      errors (doseq [e errors] (println e))
      help (println (str "Usage:\n" summary))
      version (print-version)

      :else
      (try
        (some->> arguments
                 (map io/file)
                 (filter #(.exists %))
                 (map slurp)
                 (str/join "\n")
                 t.reader/read-tasks
                 t.runner/run-tasks)
        (catch Exception ex
          (println (.getMessage ex) (ex-data ex))
          (System/exit 1))))
    (System/exit 0)))
