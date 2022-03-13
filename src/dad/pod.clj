(ns dad.pod
  (:require
   [bencode.core :as bencode]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [dad.constant :as d.const]
   [dad.reader :as d.reader]
   [dad.runner :as d.runner])
  (:import
   (java.io
    PushbackInputStream)))

(def ^:private dryrun? (atom true))

(def ^:private stdin
  (PushbackInputStream. System/in))

(defn- run-tasks
  [config tasks]
  (if @dryrun?
    (d.runner/dry-run-tasks config tasks)
    (d.runner/run-tasks config tasks)))

(defn- load-file*
  [config path]
  (let [{:keys [tasks]} (d.reader/read-tasks config (slurp path))]
    (run-tasks config tasks)))

(defn- pod-bindings
  [config]
  (merge d.reader/task-configs
         d.reader/util-bindings
         {'load-file (partial load-file* config)
          'set-dryrun! #(reset! dryrun? %)}))

(defn- describe-map
  [config]
  {"format" "edn"
   "namespaces" [{"name" (str d.const/pod-name)
                  "vars" (map (fn [[k v]]
                                {"name" (str k)
                                 "meta" (-> (meta v)
                                            (select-keys [:name :doc :arglists])
                                            (pr-str))})
                              (pod-bindings config))}]})

(defn- read-string*
  [^"[B" x]
  (String. x))

(defn- read*
  []
  (bencode/read-bencode stdin))

(defn- write*
  [m]
  (bencode/write-bencode System/out m)
  (.flush System/out))

(defn- get-function
  [config msg]
  (let [var-sym (-> (get msg "var")
                    (read-string*)
                    (symbol))
        var-sym (if (qualified-symbol? var-sym)
                  (-> var-sym
                      (name)
                      (symbol))
                  var-sym)]
    (get (pod-bindings config) var-sym)))

(defn- tasks? [x]
  (and (sequential? x)
       (every? #(contains? % :type) x)))

(defn- debug
  [& strs]
  (spit "/tmp/neko" (str (str/join " " strs) "\n") :append true))

(defn- invoke
  [config id msg]
  (if-let [f (get-function config msg)]
    (try
      (let [args (-> (get msg "args")
                     (read-string*)
                     (edn/read-string))
            ret (apply f args)
            config' (assoc-in config [:log :compact?] true)

            out (when (tasks? ret)
                  (debug "FIXME" (pr-str ret))
                  (with-out-str (run-tasks config' ret)))
            _ (debug "FIXME out" (pr-str out))
            reply (cond-> {"value" (pr-str (when-not out ret))
                           "id" id
                           "status" ["done"]}
                    out (assoc "out" out))]
        (write* reply))
      (catch clojure.lang.ExceptionInfo ex
        (if (= ::d.reader/validation-error (some-> ex ex-data :type))
          (let [{:keys [args errors]} (ex-data ex)
                reply {"ex-message" (ex-message ex)
                       "ex-data" (pr-str (ex-data ex))
                       "id" id
                       "out" (str "Validation error:\n"
                                  (str/join "\n" (map #(str "- " (pr-str %)) errors))
                                  "\n\n"
                                  "Arguments:\n"
                                  "- " (pr-str args))
                       "status" ["done" "error"]}]
            (write* reply))
          (throw ex))))
    (throw (ex-info (str "Var not found: " (read-string* (get msg "var"))) {}))))

(defn start
  [config]
  (loop []
    (let [msg (try (read*)
                   (catch java.io.EOFException _
                     ::EOF))]
      (when-not (identical? ::EOF msg)
        (let [op (some-> msg
                         (get "op")
                         (read-string*)
                         (keyword))
              id (some-> msg
                         (get "id")
                         (read-string*))
              id (or id "unknown")]
          (case op
            :describe
            (do (write* (describe-map config))
                (recur))

            :invoke
            (do (try
                  (invoke config id msg)
                  (catch Throwable ex
                    (let [reply {"ex-message" (ex-message ex)
                                 "ex-data" (pr-str (assoc (ex-data ex)
                                                          :type (str (class ex))))
                                 "id" id
                                 "status" ["done" "error"]}]
                      (write* reply))))
                (recur))

            :shutdown
            (System/exit 0)

            (do (let [reply {"ex-message" "Unknown op"
                             "ex-data" (pr-str {:op op})
                             "id" id
                             "status" ["done" "error"]}]
                  (write* reply))
                (recur))))))))
