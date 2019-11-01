(ns oton.runner.template
  (:require [clojure.java.io :as io]
            [oton.runner :as o.r]))

(defmethod o.r/run-task :template
  [{:keys [path source variables] :or {variables {}} :as task}]
  (let [tmpl (-> source io/file slurp
                 (o.r/expand-task-vars variables))]
    (spit path tmpl)
    (o.r/run-default task)))
