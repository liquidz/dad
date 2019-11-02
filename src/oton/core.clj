(ns oton.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [oton.config :as o.config]
            [oton.logger :as o.log]
            [oton.os :as o.os]
            [oton.reader :as o.reader]
            [oton.runner :as o.runner]))

(def ^:private cli-options
  [["-s" "--silent"]
   [nil "--debug"]
   ["-h" "--help"]
   ["-v" "--version"]])

(defn- print-version []
  (let [ver (-> "version.txt" io/resource slurp str/trim)]
    (println (str "oton ver " ver))
    (println (str "* Detected OS: " (name o.os/os-type)))))

(defn- usage [summary]
  (print-version)
  (println (str "* Usage:\n" summary)))

(defn- show-read-tasks [tasks]
  (doseq [task tasks]
    (println (dissoc task :id))))

(defn -main [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [debug help silent version]} options
        config (o.config/read-config)
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
                       (o.reader/read-tasks config)
                       show-read-tasks)
              (catch Exception ex
                (println (.getMessage ex) (ex-data ex))
                (System/exit 1)))

      :else
      (binding [o.log/*level* log-level]
        (try
          (some->> arguments
                   (map slurp)
                   (str/join "\n")
                   (o.reader/read-tasks config)
                   (o.runner/run-tasks config))
          (catch Exception ex
            (println (.getMessage ex) (ex-data ex))
            (System/exit 1)))))
    (System/exit 0)))
