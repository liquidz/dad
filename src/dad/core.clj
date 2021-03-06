(ns dad.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [dad.config :as d.config]
   [dad.logger :as d.log]
   [dad.nrepl :as d.nrepl]
   [dad.os :as d.os]
   [dad.reader :as d.reader]
   [dad.repl :as d.repl]
   [dad.runner :as d.runner])
  (:import
   java.net.ServerSocket))

(defn- empty-port
  []
  (with-open [sock (ServerSocket. 0)]
    (.getLocalPort sock)))

(def ^:private cli-options
  [[nil,  "--debug",     "Debug mode"]
   [nil,  "--dry-run",   "Check whether recipes will change your environment"]
   ["-e", "--eval CODE", "Evaluate a code"]
   ["-h", "--help",      "Print this help text"]
   [nil,  "--no-color",  "Disable colorize"]
   [nil,  "--repl",      "Start REPL(dry-run mode)"]
   [nil,  "--nrepl",     "Start nREPL(dry-run mode)"]
   ["-p", "--port PORT", "Port number for nREPL" :default (empty-port) :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-s", "--silent",    "Silent mode"]
   ["-v", "--version",   "Print version"]])

(defn- print-version
  [config]
  (println (str (:name config) " v" (d.config/version)))
  (println (str "* Detected OS: " (name (d.os/os-type)))))

(defn- usage
  [config summary]
  (print-version config)
  (println "")
  (println "Usage:")
  (println (str "  " (str/lower-case (:name config)) " [options] [recipe files]"))
  (println "")
  (println (str "Options:\n" summary)))

(defn- fetch-codes-by-arguments
  [arguments options]
  (let [codes (some->> (seq arguments)
                       (map slurp)
                       (str/join "\n"))]
    (if-let [eval-code (:eval options)]
      (str eval-code " " codes)
      codes)))

(defn- fetch-codes-by-stdin
  []
  (->> *in*
       io/reader
       line-seq
       (str/join "\n")))

(defn -main
  [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [debug dry-run no-color repl nrepl port help silent version]} options
        config (d.config/read-config)
        log-level (cond
                    silent :silent
                    debug :debug
                    :else :info)
        runner-fn (if dry-run
                    d.runner/dry-run-tasks
                    d.runner/run-tasks)]
    (binding [d.log/*level* log-level
              d.log/*color* (not no-color)]
      (cond
        errors (do (doseq [e errors] (println e))
                   (usage config summary)
                   (System/exit 1))
        help (usage config summary)
        version (print-version config)
        repl (->> (fetch-codes-by-arguments arguments options)
                  (d.repl/start-loop config))
        nrepl (do (-> config
                      (assoc-in [:nrepl :port] port)
                      d.nrepl/start-server)
                  @(promise))

        :else
        (try
          (some->> (or (fetch-codes-by-arguments arguments options)
                       (fetch-codes-by-stdin))
                   (d.reader/read-tasks config)
                   :tasks
                   (runner-fn config))
          (catch Exception ex
            (d.log/error (.getMessage ex) (ex-data ex))
            (System/exit 1)))))
    (System/exit 0)))
