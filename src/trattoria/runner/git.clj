(ns trattoria.runner.git
  (:require [clojure.java.io :as io]
            [trattoria.runner :as r]))

(defmethod r/run-task :git
  [{:keys [path] :as task}]
  (let [task (cond-> task
               (some-> path io/file .exists) (assoc :type :git-checkout))]
    (r/run-default task)))
