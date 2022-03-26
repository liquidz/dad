(ns dad.core
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :as cli]
   [dad.config :as d.config]
   [dad.constant :as d.const]
   [dad.logger :as d.log]
   [dad.os :as d.os]
   [dad.pod :as d.pod]
   [dad.reader :as d.reader]
   [dad.repl :as d.repl]
   [dad.runner :as d.runner]))

(def ^:private cli-options
  [[nil,  "--debug",     "Debug mode"]
   [nil,  "--dry-run",   "Check whether recipes will change your environment"]
   ["-e", "--eval CODE", "Evaluate a code"]
   ["-h", "--help",      "Print this help text"]
   [nil,  "--init NAME", "FIXME"]
   [nil,  "--no-color",  "Disable colorize"]
   [nil,  "--repl",      "Start REPL(dry-run mode)"]
   ["-s", "--silent",    "Silent mode"]
   ["-v", "--version",   "Print version"]])

(defn- print-version
  [config]
  (println (str (:name config) " v" (:version config)))
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
  (let [rdr (io/reader *in*)]
    (when (.ready rdr)
      (->> rdr
           line-seq
           (str/join "\n")))))

(defn- complete-require-code
  [code]
  (if (str/includes? (str code) (str d.const/pod-name))
    code
    (str d.const/require-refer-all-code
         code)))

(defn- generate-template
  [namespace-name]
  (try
    (let [arr  (str/split namespace-name #"\.")
          file (apply io/file (update arr (dec (count arr)) #(str % ".clj")))
          content (format (slurp (io/resource "template.clj")) namespace-name)]
      (.mkdirs (.getParentFile file))
      (spit file content)
      file)
    (catch Exception ex
      (d.log/error (.getMessage ex) (ex-data ex)))))

(defn -main
  [& args]
  (let [{:keys [arguments options summary errors]} (cli/parse-opts args cli-options)
        {:keys [debug dry-run no-color repl help silent version init]} options
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
        errors
        (do (doseq [e errors] (println e))
            (usage config summary)
            (System/exit 1))

        help
        (usage config summary)

        version
        (print-version config)

        repl
        (->> (fetch-codes-by-arguments arguments options)
             (complete-require-code)
             (d.repl/start-loop config))

        init
        (when-let [file (generate-template init)]
          (d.log/info "Initialized" (.getPath file)))

        (System/getenv "BABASHKA_POD")
        (d.pod/start config)

        :else
        (try
          (let [codes (or (fetch-codes-by-arguments arguments options)
                          (fetch-codes-by-stdin))]
            (if (seq codes)
              (some->> codes
                       (complete-require-code)
                       (d.reader/read-tasks config)
                       :tasks
                       (runner-fn config))
              (d.repl/start-loop config nil)))
          (catch Exception ex
            (d.log/error (.getMessage ex) (ex-data ex))
            (System/exit 1)))))
    (System/exit 0)))
