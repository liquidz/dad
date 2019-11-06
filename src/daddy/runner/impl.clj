(ns daddy.runner.impl
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [daddy.util :as d.util]))

(defmulti transform-task :type)
(defmethod transform-task :default [task] task)

(defmethod transform-task :directory
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :directory-delete)))

(defmethod transform-task :execute
  [{:keys [command cwd] :as task}]
  (let [task (->> command
                  d.util/ensure-seq
                  (str/join " && ")
                  (assoc task :command))]
    (cond-> task
      cwd (assoc :type :execute-at))))

(defmethod transform-task :file
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :file-delete)))

(defmethod transform-task :git
  [{:keys [path] :as task}]
  (cond-> task
    (some-> path io/file .exists) (assoc :type :git-checkout)))

(defmethod transform-task :package
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:uninstall :remove} action) (assoc :type :package-uninstall)))


(defmulti do-task! :type)
(defmethod do-task! :default [task] task)

(defmethod do-task! :template
  [{:keys [path source variables] :or {variables {}}}]
  (let [source-file (io/file source)]
    (when-not (.exists source-file)
      (let [tmpl (-> source-file slurp (d.util/expand-map-to-str variables))]
        (spit path tmpl)))))
