(ns dad.pod
  (:require
   [bencode.core :as bencode]
   [clojure.edn :as edn]
   [dad.const :as d.const]
   [dad.reader :as d.reader]
   [dad.runner :as d.runner])
  (:import
   (java.io
    PushbackInputStream)))

(def ^:private stdin
  (PushbackInputStream. System/in))

(def ^:private describe-map
  {"format" "edn"
   "namespaces" [{"name" (str d.const/pod-name)
                  "vars" (map #(hash-map "name" (str %)) (keys d.reader/task-configs))}]})

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
  [msg]
  (let [qualified-var-sym (-> (get msg "var")
                              (read-string*)
                              (symbol))
        var-sym (-> qualified-var-sym
                    (name)
                    (symbol))]
    (get-in d.reader/task-configs
            [var-sym :destination])))

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
            (do (write* describe-map)
                (recur))

            :invoke
            (do (try
                  (if-let [f (get-function msg)]
                    (let [args (-> (get msg "args")
                                   (read-string*)
                                   (edn/read-string))
                          ret (apply f args)
                          out (with-out-str (d.runner/dry-run-tasks config [ret]))
                          reply {"value" (pr-str nil)
                                 "out" out
                                 "id" id
                                 "status" ["done"]}]
                      (write* reply))
                    (throw (ex-info (str "Var not found: " (get msg "var")) {})))
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
