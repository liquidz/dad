(ns dad.reader
  (:require [camel-snake-kebab.core :as csk]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [dad.logger :as d.log]
            [malli.core :as m]
            [malli.error :as me]
            [dad.util :as d.util]
            [dad.os :as d.os]
            [dad.reader.impl :as d.r.impl]
            [sci.core :as sci]))

(declare read-tasks)

(def ^:private task-configs
  {'directory, {:destination d.r.impl/directory
                :resource-name-key :path}
   'execute,   {:destination d.r.impl/execute
                :resource-name-key :command}
   'file,      {:destination d.r.impl/file
                :resource-name-key :path}
   'git,       {:destination d.r.impl/git
                :resource-name-key :path}
   'download,  {:destination d.r.impl/download
                :resource-name-key :path}
   'link,      {:destination d.r.impl/link
                :resource-name-key :path}
   'package,   {:destination d.r.impl/package
                :resource-name-key :name}
   'template,  {:destination d.r.impl/template
                :resource-name-key :path}})

(def ^:private util-bindings
  {'env,          #(get (System/getenv) (csk/->SCREAMING_SNAKE_CASE_STRING %))
   'file-exists?, #(some-> % io/file (.exists))
   'os-type,      (fn [] (name (d.os/os-type)))
   'println,      println
   'str/join,     str/join})

(defn- validate [value schema]
  (if-let [err (some-> schema
                       (m/explain value)
                       me/humanize)]
    (throw (ex-info "validation error" err))
    value))

(defn- dispatch* [task-config & args]
  (let [{:keys [schema destination resource-name-key]} task-config
        [resource-name m] (cond->> args
                            (some-> args first map?) (cons nil))
        arg-map (when (or (nil? m) (map? m))
                  (cond-> (or m {})
                    resource-name (assoc resource-name-key resource-name)))
        arg-map (validate arg-map schema)]
    (destination arg-map)))

(defn- task-id [m]
  (->> m
       (sort-by (comp str first)) ; sort by map key
       (map (comp str second)) ; extract map val
       (str/join "")))

(defn- ensure-task-list [x]
  (->> (d.util/ensure-seq x)
       (remove nil? )
       (map #(assoc % :id (task-id %)))))

(defn- load-file* [ctx path]
  (-> path
      slurp
      (sci/eval-string ctx)))

(defn- build-task-bindings [tasks-atom config]
  (letfn [(add-tasks [tasks]
            (doseq [task (ensure-task-list tasks)]
              (d.log/debug "Read task" task)
              (swap! tasks-atom conj task)))]
    (reduce-kv
     (fn [res k v]
       (let [schema (get-in config [:schema k])
             task-config (assoc v :schema schema)]
         (when-not schema
           (throw (ex-info "no validation schema error" {:name k})))
         (assoc res k (comp add-tasks (partial dispatch* task-config)))))
     {} task-configs)))

(defn read-tasks [config code-str]
  (let [tasks (atom [])
        env (atom {})
        ctx {:bindings (merge (build-task-bindings tasks config)
                              util-bindings)
             :env env}
        ctx (update ctx :bindings assoc 'load-file (partial load-file* ctx))]
    (doall (sci/eval-string code-str ctx))
    (d.util/distinct-by :id @tasks)))
