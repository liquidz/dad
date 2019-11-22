(ns daddy.runner
  (:require [clojure.java.shell :as sh]
            [daddy.logger :as d.log]
            [daddy.runner.impl :as d.r.impl]
            [daddy.util :as d.util]))

(def ^:private main-arg-candidates
  (juxt :path :name :command))

(defn- extract-main-arg [task]
  (some identity (main-arg-candidates task)))

(defn- sh [cmd]
  (d.log/debug "Running command" {:command cmd})
  (sh/sh "sh" "-c" cmd))

(defn- succeeded? [sh-result]
  (let [results (->> sh-result
                     d.util/ensure-seq
                     (remove nil?))]
    (or (empty? results)
        (every? #(= 0 (:exit %)) results))))

(defn- failed? [sh-results]
  (not (succeeded? sh-results)))

(defn- expand-task [config task]
  (if-let [task-def (get-in config [:command (:type task)])]
    (for [cmd (d.util/ensure-seq (:command task-def))]
      (if (keyword? cmd)
        (expand-task config (assoc task :type cmd))
        (assoc task :__def__ (assoc task-def :command cmd))))
    (throw (ex-info "Unknown command type" {:task task}))))

(defn- expand-tasks [config tasks]
  (->> tasks
       (map #(expand-task config %))
       flatten))

(defn- has-enough-params? [expanded-task]
  (let [requires (get-in expanded-task [:__def__ :requires] [])]
    (or (empty? requires)
        (every? #(contains? expanded-task %) requires))))

(defn- distinct-once [tasks]
  (->> tasks
       (reduce (fn [{:keys [onces] :as res} task]
                 (let [once? (get-in task [:__def__ :once?] false)
                       exists? (and once? (contains? onces (:type task)))]
                   (cond
                     (and once? exists?) res
                     (and once? (not exists?)) (-> res
                                                   (update :result conj task)
                                                   (update :onces conj (:type task)))
                     (not once?) (update res :result conj task))))
               {:result [] :onces #{}})
       :result))

(defn- task->command [expanded-task]
  (when-let [command (get-in expanded-task [:__def__ :command])]
    (d.util/expand-map-to-str command (dissoc expanded-task :type :__def__))))

(defn- run-task* [expanded-task]
  (some-> expanded-task
          d.r.impl/run-by-code
          task->command
          sh))

(defn- expand-pre-tasks [config expanded-task]
  (let [task-def (:__def__ expanded-task)]
    (when-let [pre-task (or (:pre task-def) (:pre-not task-def))]
      (->> (d.util/ensure-seq pre-task)
           (map #(if (keyword? %)
                   (expand-tasks config [(assoc expanded-task :type %)])
                   (assoc expanded-task :__def__ (-> task-def
                                                     (assoc :command %)
                                                     (dissoc :pre :pre-not)))))
           flatten))))

(defn- runnable? [config expanded-task]
  (let [task-def (:__def__ expanded-task)]
    (if-let [pre-tasks (expand-pre-tasks config expanded-task)]
      (cond-> (and (every? has-enough-params? pre-tasks)
                   (every? succeeded? (map run-task* pre-tasks)))
        (contains? task-def :pre-not) not)
      true)))

(defn- run-task [config expanded-task]
  (d.log/debug "Start to run task" {:task expanded-task})
  (if (runnable? config expanded-task)
    (let [res (run-task* expanded-task)]
      (d.log/debug "Finish to run task" {:task expanded-task})
      (if (failed? res)
        (throw (ex-info "Failed to run command" {:fixme expanded-task}))
        (do (d.log/info (format "%s [%s]"
                                (name (:type expanded-task))
                                (extract-main-arg expanded-task)))
            res)))
    (d.log/debug "Not runnable task" {:task expanded-task})))

(defn run-tasks [config tasks]
  (d.log/info "Daddy started cooking...")
  (->> tasks
       (map d.r.impl/dispatch-task)
       (expand-tasks config)
       (filter has-enough-params?)
       distinct-once
       (map (partial run-task config))
       (remove nil?)
       doall))
