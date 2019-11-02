(ns oton.runner.impl
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [oton.util :as o.util]))

(defmulti pre-task :type)
(defmethod pre-task :default [task] task)

(defmethod pre-task :directory
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :directory-delete)))

(defmethod pre-task :execute
  [{:keys [command cwd] :as task}]
  (let [task (->> command
                  o.util/ensure-seq
                  (str/join " && ")
                  (assoc task :command))]
    (cond-> task
      cwd (assoc :type :execute-at))))

(defmethod pre-task :file
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :file-delete)))

(defmethod pre-task :git
  [{:keys [path] :as task}]
  (cond-> task
    (some-> path io/file .exists) (assoc :type :git-checkout)))

(defmethod pre-task :package
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:uninstall :remove} action) (assoc :type :package-uninstall)))

(defmethod pre-task :template
  [{:keys [path source variables] :or {variables {}} :as task}]
  (let [source-file (io/file source)]
    (when-not (.exists source-file)
      (let [tmpl (-> source-file slurp (o.util/expand-map-to-str variables))]
        (spit path tmpl)
        task))))
