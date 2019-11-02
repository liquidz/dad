(ns oton.runner
  (:require [clojure.java.shell :as sh]
            [oton.logger :as o.log]
            [oton.runner.impl :as o.r.impl]
            [oton.util :as o.util]))

(def ^:private context (atom {}))
(declare run-task)

(defn init-context! [config]
  (reset! context {:config config
                   :run-commands #{}}))

(defn sh [cmd]
  (o.log/debug "Running command" {:command cmd})
  (sh/sh "sh" "-c" cmd))

(defn succeeded? [sh-result]
  (let [results (->> sh-result
                     o.util/ensure-seq
                     (remove nil?))]
    (or (empty? results)
        (every? #(= 0 (:exit %)) results))))

(defn failed? [sh-results]
  (not (succeeded? sh-results)))

(defn construct-commands [task]
  (let [{:keys [config run-commands]} @context
        task-def (get-in config [:command (:type task)])
        _ (when-not task-def
            (throw (ex-info "Failed to find command definition" {:task task})))
        {:keys [requires command once?] :or {once? false}} task-def]
    (when (and (or (empty? requires)
                   (every? #(contains? task %) requires))
               (or (not once?)
                   (not (contains? run-commands command))))
      (when once?
        (swap! context update-in :run-commands conj command))
      (->> (o.util/ensure-seq command)
           (map #(if (keyword? %)
                   (construct-commands (assoc task :type %))
                   (o.util/expand-map-to-str % (dissoc task :type))))
           flatten
           (remove nil?)))))

(defn run? [task]
  (if-let [pre-task (or (:pre task) (:pre-not task))]
    (cond-> (succeeded? (run-task (assoc task :command pre-task)))
      (contains? task :pre-not) not)
    true))

(defn run-task* [task]
  (o.log/debug "Running task" {:task task})
  (when-let [commands (construct-commands task)]
    (when (run? task)
      (doall (map sh commands)))))

(defn run-task [task]
  (some-> task
          o.r.impl/pre-task
          run-task*))

(defn run-tasks [config tasks]
  (init-context! config)
  (->> tasks
       (mapcat (fn [task]
                 (o.log/info "Start to run task" {:task task})
                 (let [res (run-task task)]
                   (when (failed? res)
                     (throw (ex-info "Failed to run command" {:task task :result res})))
                   (o.log/info "Finish to run task" {:task task})
                   res)))
       doall))
