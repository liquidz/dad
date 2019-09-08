(ns trattoria.runner.template
  (:require [trattoria.runner :as r]
            [clojure.java.io :as io]))

(defmethod r/run-task :template
  [{:keys [path source variables] :or {variables {}} :as task}]
  (let [tmpl (-> source io/file slurp
                 (r/expand-task-vars variables))]
    (spit path tmpl)
    (r/run-default task)))


(comment (r/run-task {:type :template
                      :path "/tmp/nekotmp"
                      :source "hoge.tmpl"
                      :variables {:body "!!!!!"}
                      }))
