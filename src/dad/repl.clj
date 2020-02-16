(ns dad.repl
  (:require [clojure.string :as str]
            [dad.config :as d.config]
            [dad.reader :as d.reader]
            [dad.runner :as d.runner]))

(defn- reset-env! [env]
  (swap! env update-in [:namespaces 'user]
         #(apply dissoc % (keys d.reader/task-configs)))
  env)

(defn- eval*
  "Return false when continuing reading line is required."
  [config code]
  (try
    (let [{:keys [res tasks]} (d.reader/read-tasks config code)]
      (if-let [tasks (seq tasks)]
        (d.runner/dry-run-tasks config tasks)
        (println res))
      true)
    (catch Exception ex
      (if (-> ex ex-data :type
              #{:edamame/error :reader-exception})
        ;; parse error, continue to reading line
        false
        (do (println (.getMessage ex))
            true)))))

(defn start-loop [config init-codes]
  (let [env (atom {})
        config (assoc config
                      :env env
                      :log {:compact? true})
        {:keys [prompt exit-codes]} (:repl config)
        exit-code-set (set exit-codes)]
    (println (str (:name config)
                  " v" (d.config/version)
                  " REPL"))
    (println "Please note that evaluations in this REPL *DO NOT AFFECT* your environment.")

    (println (str "  Docs: (dad/doc) or (dad/doc \"name\")\n"
                  "        (help) is an alias for (dad/doc)"))
    (println (str "  Exit: " (str/join " or " exit-codes)
                  " to quit this REPL."))
    (println "")

    (when (seq init-codes)
      (d.reader/read-tasks config init-codes)
      (reset-env! env))

    (loop [last-line "" need-prompt? true]
      (when need-prompt?
        (print prompt)
        (flush))
      (when-let [line (some-> (read-line) str/trim)]
        (when-not (exit-code-set line)
          (let [line (cond->> line
                       (not need-prompt?) (str last-line "\n"))
                need-prompt? (eval* config line)]
            (reset-env! env)
            (recur line need-prompt?)))))))
