(ns trattoria.runner.execute
  (:require [clojure.string :as str]
            [trattoria.runner :as r]))

(defmethod r/run-task :execute
  [{:keys [cwd command] :as task}]
  (let [cmd (-> (str/join " && " (r/ensure-seq command))
                (r/expand-task-vars task))]
    (r/sh
     (if cwd
       (format "(cd %s && %s)" cwd cmd)
       (format "(%s)" cmd)))))
