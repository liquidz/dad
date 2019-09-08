(ns trattoria.runner
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [trattoria.logger :as log]
            [trattoria.os :as t.os]))

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
  (let [x (some-> (io/resource (str "command/" (name t.os/os-type) ".edn"))
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
  (when-let [{:keys [command requires]} (find-command-def task)]
    (when (or (empty? requires)
              (every? #(contains? task %) requires))
      (->> (for [cmd (ensure-seq command)]
             (if (keyword? cmd)
               (construct-commands (assoc task :type cmd))
               (expand-task-vars cmd task)))
           flatten
           (remove nil?)))))

(defn run-default [task]
  (log/debug "Running default task" {:task task})
  (if-let [commands (construct-commands task)]
    (doseq [cmd commands]
      (let [res (sh cmd)]
        (when (failed? res)
          (throw (ex-info "Failed to run command" {:task task :command cmd :result res})))))
    (throw (ex-info "Failed to find command definition" {:task task}))))

(defmulti run-task :type)
(defmethod run-task :default
  [task]
  (throw (ex-info "Unknown task" {:task task})))

(defn run-tasks [tasks]
  (doseq [task tasks]
    (log/info "Start to run task" {:task task})
    (run-task task)
    (log/info "Finish to run task" {:task task})))
