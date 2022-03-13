(ns dad.runner
  (:require
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [dad.logger :as d.log]
   [dad.runner.impl :as d.r.impl]
   [dad.util :as d.util]))

(declare expand-tasks
         run-task*)

(def ^:private main-arg-candidates
  (juxt :title :path :name :command))

(defn- extract-main-arg
  [task]
  (some identity (main-arg-candidates task)))

(defn- sh
  [cmd]
  (d.log/debug "Running command" {:command cmd})
  (sh/sh "sh" "-c" cmd))

(defn- compact-log?
  [config]
  (get-in config [:log :compact?] false))

(defn- succeeded?
  [sh-result]
  (let [results (->> sh-result
                     d.util/ensure-seq
                     (remove nil?))]
    (or (empty? results)
        (every? #(= 0 (:exit %)) results))))

(defn- failed?
  [sh-results]
  (not (succeeded? sh-results)))

(defn- get-task-def
  ([config task]
   (get-task-def config task {}))
  ([config task default]
   (get-in config [:command (:type task)] default)))

(defn- has-enough-params?
  ([config task]
   (let [requires (get (get-task-def config task) :requires [])]
     (or (empty? requires)
         (every? #(contains? task %) requires))))
  ([expanded-task]
   (let [requires (get-in expanded-task [:__def__ :requires] [])]
     (or (empty? requires)
         (every? #(contains? expanded-task %) requires)))))

(defn- expand-pre-tasks
  [config task]
  (let [task-def (get-task-def config task)]
    (when-let [pre-task (or (:pre task-def) (:pre-not task-def)
                            (:pre task) (:pre-not task))]
      (->> (d.util/ensure-seq pre-task)
           (map #(if (keyword? %)
                   (expand-tasks config [(assoc task :type %)])
                   (assoc task :__def__ (-> task-def
                                            (assoc :command %)
                                            (dissoc :pre :pre-not)))))
           flatten))))

(defn- runnable?
  [config task]
  (let [task-def (get-task-def config task)
        pre-not? (or (contains? task-def :pre-not)
                     (contains? task :pre-not))]
    (if-let [pre-tasks (expand-pre-tasks config task)]
      (cond-> (and (every? #(has-enough-params? config %) pre-tasks)
                   (every? succeeded? (map run-task* pre-tasks)))
        pre-not? not)
      true)))

(defn- expand-task
  [config task]
  (if-let [task-def (get-in config [:command (:type task)])]
    (if (runnable? config task)
      (for [cmd (d.util/ensure-seq (:command task-def))]
        (if (keyword? cmd)
          (expand-task config (assoc task :type cmd))
          (assoc task :__def__ (assoc task-def :command cmd))))
      (assoc task :not-runnable? true))
    (throw (ex-info "Unknown command type" {:task task}))))

(defn- expand-tasks
  [config tasks]
  (->> tasks
       (map #(expand-task config %))
       (flatten)
       (remove nil?)))

(defn- distinct-once
  [tasks]
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

(defn- task->command
  [expanded-task]
  (when-let [command (get-in expanded-task [:__def__ :command])]
    (d.util/expand-map-to-str command (dissoc expanded-task :type :__def__))))

(defn- run-task*
  [expanded-task]
  (let [res (some-> expanded-task
                    d.r.impl/run-by-code)]
    (if res
      (some-> res task->command sh)
      {:exit -1})))

(defn- generate-info-log
  [expanded-task]
  (let [type-name (name (:type expanded-task))
        main-arg (extract-main-arg expanded-task)]
    (format "  %s [%s] ... " type-name main-arg)))

(defn- start-info-log
  [config expanded-task]
  (let [compact? (compact-log? config)]
    (cond->> (generate-info-log expanded-task)
      compact-log?
      (str/triml)

      (not compact?)
      (d.log/message :info)

      :always
      (d.log/info*))))

(defn- finish-info-log
  [result]
  (let [msg (if (failed? result)
              (d.log/colorize :red "ng")
              (d.log/colorize :green "ok"))]
    (d.log/info* (str msg "\n"))))

(defn- run-task
  [config expanded-task]
  (if (runnable? config expanded-task)
    (do (start-info-log config expanded-task)
        (let [res (run-task* expanded-task)]
          (finish-info-log res)
          (if (failed? res)
            (throw (ex-info "Failed to run command" {:result res :task (dissoc expanded-task :__def__)}))
            res)))
    (d.log/debug "Not runnable task" {:task expanded-task})))

(defn run-tasks
  [config tasks]
  (when-not (compact-log? config)
    (d.log/info "Dad started cooking."))
  (->> tasks
       (map d.r.impl/dispatch-task)
       (expand-tasks config)
       (filter has-enough-params?)
       (remove :not-runnable?)
       distinct-once
       (map (partial run-task config))
       (remove nil?)
       doall))

(defn dry-run-tasks
  [config tasks]
  (let [compact? (compact-log? config)]
    (when-not compact?
      (d.log/info "Dad started tasting."))
    (doseq [task (->> tasks
                      (map d.r.impl/dispatch-task)
                      (expand-tasks config)
                      (filter has-enough-params?)
                      distinct-once)]
      (cond-> (generate-info-log task)
        compact? str/triml
        :always d.log/info*)
      (d.log/info* (str (if (:not-runnable? task)
                          (d.log/colorize :red "WILL NOT change")
                          (d.log/colorize :green "will change"))
                        "\n")))))
