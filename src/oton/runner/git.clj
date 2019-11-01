(ns oton.runner.git
  (:require [clojure.java.io :as io]
            [oton.runner :as o.r]))

(defmethod o.r/run-task :git
  [{:keys [path] :as task}]
  (let [task (cond-> task
               (some-> path io/file .exists) (assoc :type :git-checkout))]
    (o.r/run-default task)))
