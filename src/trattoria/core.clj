(ns trattoria.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [trattoria.logger :as t.logger]
            [trattoria.os :as t.os]
            [trattoria.reader :as t.reader]
            [trattoria.runner :as t.runner]
            [trattoria.runner.directory]
            [trattoria.runner.execute]
            [trattoria.runner.file]
            [trattoria.runner.git]
            [trattoria.runner.link]
            [trattoria.runner.package]
            [trattoria.runner.template]))

(def ^:private cli-options
  [["-s" "--silent"]
   [nil "--debug"]
   ["-h" "--help"]
   ["-v" "--version"]])

(defn- print-version []
  (let [ver (-> "version.txt" io/resource slurp str/trim)]
    (println (str "trattoria ver " ver))
    (println (str "* Detected OS: " (name t.os/os-type)))))

(defn- usage [summary]
  (print-version)
  (println (str "* Usage:\n" summary)))

(defn- show-read-tasks [tasks]
  (doseq [task tasks]
    (println (dissoc task :id))))

(defn -main [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [debug help silent version]} options
        log-level (cond
                    silent :silent
                    debug :debug
                    :else :info)]
    (cond
      errors (doseq [e errors] (println e))
      help (usage summary)
      version (print-version)
      debug (try
              (some->> arguments
                       (map slurp)
                       (str/join "\n")
                       t.reader/read-tasks
                       show-read-tasks
                       )
              (catch Exception ex
                (println (.getMessage ex) (ex-data ex))
                (System/exit 1)))

      :else
      (binding [t.logger/*level* log-level]
        (try
          (some->> arguments
                   (map slurp)
                   (str/join "\n")
                   t.reader/read-tasks
                   t.runner/run-tasks)
          (catch Exception ex
            (println (.getMessage ex) (ex-data ex))
            (System/exit 1)))))
    (System/exit 0)))
