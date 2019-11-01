(ns oton.runner
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [oton.logger :as log]
            [oton.os :as o.os]))

(def ^:private run-commands (atom #{}))

(defn ensure-seq [x]
  (cond-> x
    (not (sequential? x)) vector))

(defn sh [cmd]
  (log/debug "Running command" {:command cmd})
  (sh/sh "sh" "-c" cmd))

(defn succeeded? [sh-result]
  (= 0 (:exit sh-result)))
(defn failed? [sh-result]
  (not= 0 (:exit sh-result)))

(defn find-command-def [task]
  (let [x (some-> (io/resource (str "command/" (name o.os/os-type) ".edn"))
                  slurp edn/read-string)
        y (some-> (io/resource "command/base.edn")
                  slurp edn/read-string)]
    (get x (:type task)
         (get y (:type task)))))

(defn expand-task-vars [s task]
  (reduce-kv
   (fn [res k v]
     (str/replace res (str "%" (name k) "%") (str v)))
   s (dissoc task :type)))

(defn construct-commands [task]
  (if-let [{:keys [command requires once?] :or {once? false}} (find-command-def task)]
    (when (and (or (empty? requires)
                   (every? #(contains? task %) requires))
               (or (not once?)
                   (not (contains? @run-commands command))))
      (when once? (swap! run-commands conj command))
      (flatten
       (for [cmd (ensure-seq command)]
         (if (keyword? cmd)
           (construct-commands (assoc task :type cmd))
           (expand-task-vars cmd task)))))
    (throw (ex-info "Failed to find command definition" {:task task}))))

(defn run-default [task]
  (log/debug "Running default task" {:task task})
  (when-let [commands (construct-commands task)]
    (doseq [cmd (remove nil? commands)]
      (let [res (sh cmd)]
        (when (failed? res)
          (throw (ex-info "Failed to run command" {:task task :command cmd :result res})))))))

(defmulti run-task :type)
(defmethod run-task :default
  [task]
  (throw (ex-info "Unknown task" {:task task})))

(defn run-tasks [tasks]
  (reset! run-commands #{})
  (doseq [task tasks]
    (log/info "Start to run task" {:task task})
    (run-task task)
    (log/info "Finish to run task" {:task task})))
