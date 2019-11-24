(ns daddy.runner.impl
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [daddy.util :as d.util]))

(defmulti dispatch-task :type)
(defmethod dispatch-task :default [task] task)

(defmethod dispatch-task :directory
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :directory-delete)))

(defmethod dispatch-task :execute
  [{:keys [command cwd] :as task}]
  (let [task (->> command
                  d.util/ensure-seq
                  (str/join " && ")
                  (assoc task :command))]
    (cond-> task
      cwd (assoc :type :execute-at))))

(defmethod dispatch-task :file
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :file-delete)))

(defmethod dispatch-task :git
  [{:keys [path] :as task}]
  (cond-> task
    (some-> path io/file .exists) (assoc :type :git-checkout)))

(defmethod dispatch-task :package
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:uninstall :remove} action) (assoc :type :package-uninstall)))

(defmulti run-by-code :type)
(defmethod run-by-code :default [task] task)

(defmethod run-by-code :template
  [{:keys [path source variables] :or {variables {}} :as task}]
  (let [source-file (io/file source)]
    (when (.exists source-file)
      (let [tmpl (-> source-file slurp (d.util/expand-map-to-str variables "{{" "}}"))]
        (spit path tmpl)
        task))))
