(ns oton.runner.file
  (:require [oton.runner :as o.r]))

(defmethod o.r/run-task :file
  [{:keys [action] :as task}]
  (o.r/run-default
   (cond-> task
     (contains? #{:delete :remove} action) (assoc :type :file-delete))))
