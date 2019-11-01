(ns oton.runner.package
  (:require [oton.logger :as log]
            [oton.runner :as o.r]))

(defmethod o.r/run-task :package
  [{:keys [action] :as task}]
  (log/info "Checking package existence" {:name (:name task)})
  (if-let [pre (some-> (assoc task :type :package-exist?)
                       o.r/construct-commands first o.r/sh)]
    (if (o.r/failed? pre)
      ;; not existing
      (when (= :install action)
        (log/info "Installing package" {:name (:name task)})
        (o.r/run-default task))
      ;; existing
      (when (contains? #{:uninstall :remove} action)
        (log/info "Uninstalling package" {:name (:name task)})
        (o.r/run-default (assoc task :type :package-uninstall))))
    (throw (ex-info "Failed to check package existence" {:task task}))))
