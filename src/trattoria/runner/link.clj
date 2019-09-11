(ns trattoria.runner.link
   (:require [trattoria.runner :as r]))

(defmethod r/run-task :link
  [task]
  (r/run-default task))
