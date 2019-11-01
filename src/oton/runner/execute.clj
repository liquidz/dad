(ns oton.runner.execute
  (:require [clojure.string :as str]
            [oton.runner :as o.r]))

(defmethod o.r/run-task :execute
  [{:keys [cwd command] :as task}]
  (let [cmd (-> (str/join " && " (o.r/ensure-seq command))
                (o.r/expand-task-vars task))]
    (o.r/sh
     (if cwd
       (format "(cd %s && %s)" cwd cmd)
       (format "(%s)" cmd)))))
