(ns oton.runner.directory
  (:require [oton.runner :as o.r]))

(defmethod o.r/run-task :directory
  [{:keys [action] :as task}]
  (o.r/run-default
   (cond-> task
     (contains? #{:delete :remove} action) (assoc :type :directory-delete))))
