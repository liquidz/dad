(ns oton.runner.link
   (:require [oton.runner :as o.r]))

(defmethod o.r/run-task :link
  [task]
  (o.r/run-default task))
