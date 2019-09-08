(ns trattoria.runner.git
  (:require [clojure.java.io :as io]
            [trattoria.runner :as r]))

(defmethod r/run-task :git
  [{:keys [path] :as task}]
  (let [task (cond-> task
               (.exists (io/file path)) (assoc :type :git-checkout))]
    (r/run-default task)))
