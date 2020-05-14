(ns dad.runner.impl
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [dad.logger :as d.log]
   [dad.util :as d.util]))

(defmulti dispatch-task :type)
(defmethod dispatch-task :default [task] task)

(defmethod dispatch-task :directory
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :directory-delete)))

(defmethod dispatch-task :execute
  [{:keys [command pre pre-not cwd] :as task}]
  (let [transform #(when-let [s (some->> % d.util/ensure-seq (str/join " && "))]
                     (if cwd
                       (format "cd %s && %s" cwd s)
                       s))
        [command pre pre-not] (map transform [command pre pre-not])]
    (cond-> (assoc task :command command)
      pre (assoc :pre pre)
      pre-not (assoc :pre-not pre-not))))

(defmethod dispatch-task :file
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:delete :remove} action) (assoc :type :file-delete)))

(defmethod dispatch-task :package
  [{:keys [action] :as task}]
  (cond-> task
    (contains? #{:uninstall :remove} action) (assoc :type :package-uninstall)))

(defmulti run-by-code :type)
(defmethod run-by-code :default [task] task)

(defn- render-template
  [file variables]
  (-> file slurp str/trim (d.util/expand-map-to-str variables "{{" "}}")))

(defmethod run-by-code :_pre-compare-template-content
  [{:keys [path source variables] :as task}]
  (let [path-hash (and (.exists (io/file path))
                       (-> path slurp str/trim d.util/sha256))
        source-hash (and (.exists (io/file source))
                         (-> source (render-template variables) d.util/sha256))]
    (d.log/debug "Comparing template content"
                 {:path path-hash :source source-hash})
    (if (and path-hash source-hash)
      (when (not= path-hash source-hash)
        task)
      task)))

(defmethod run-by-code :template-create
  [{:keys [path source variables] :or {variables {}} :as task}]
  (let [source-file (io/file source)]
    (when (.exists source-file)
      (spit path (render-template source-file variables))
      task)))
