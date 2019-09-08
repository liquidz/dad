(ns trattoria.runner.directory
  (:require [trattoria.runner :as r]))

(defmethod r/run-task :directory
  [{:keys [action] :as task}]
  (r/run-default
   (cond-> task
     (contains? #{:delete :remove} action) (assoc :type :directory-delete))))
