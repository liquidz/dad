(ns trattoria.runner.file
  (:require [trattoria.runner :as r]))

(defmethod r/run-task :file
  [{:keys [action] :as task}]
  (r/run-default
   (cond-> task
     (contains? #{:delete :remove} action) (assoc :type :file-delete))))
