(ns trattoria.runner.package
  (:require [trattoria.logger :as log]
            [trattoria.runner :as r]))

(defmethod r/run-task :package
  [{:keys [action] :as task}]
  (log/info "Checking package existence" {:name (:name task)})
  (if-let [pre (some-> (assoc task :type :package-exist?)
                       r/construct-commands first r/sh)]
    (if (r/failed? pre)
      ;; not existing
      (when (= :install action)
        (log/info "Installing package" {:name (:name task)})
        (r/run-default task))
      ;; existing
      (when (contains? #{:uninstall :remove} action)
        (log/info "Uninstalling package" {:name (:name task)})
        (r/run-default (assoc task :type :package-uninstall))))
    (throw (ex-info "Failed to check package existence" {:task task}))))
