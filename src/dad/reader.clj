(ns dad.reader
  (:require
   [clojure.string :as str]
   [dad.constant :as d.const]
   [dad.logger :as d.log]
   [dad.reader.impl :as d.r.impl]
   [dad.reader.util :as d.r.util]
   [dad.schema :as d.schema]
   [dad.util :as d.util]
   [malli.core :as m]
   [malli.error :as me]
   [sci.core :as sci]))

(declare read-tasks)

(defn- task-id
  [m]
  (->> m
       (sort-by (comp str first)) ; sort by map key
       (map (comp str second)) ; extract map val
       (str/join "")))

(defn- ensure-task-list
  [x]
  (->> (d.util/ensure-seq x)
       (remove nil?)
       (map #(assoc % :id (task-id %)))))

(defn wrap-task
  [function-var]
  (with-meta (fn [& args]
               (let [schema (some-> function-var
                                    (d.schema/extract-function-input-schema))]
                 (when-not schema
                   (throw (ex-info "no validation schema error" {:var function-var})))
                 (if-let [err (some-> schema
                                      (m/explain args)
                                      (me/humanize))]
                   (throw (ex-info "validation error" {:type ::validation-error
                                                       :args args
                                                       :errors err}))
                   (ensure-task-list
                    (apply function-var args)))))
    (meta function-var)))

(def task-configs
  (reduce
   (fn [accm function-var]
     (let [m (meta function-var)]
       (assoc accm (:name m) (wrap-task function-var))))
   {}
   [#'d.r.impl/directory
    #'d.r.impl/execute
    #'d.r.impl/file
    #'d.r.impl/git
    #'d.r.impl/download
    #'d.r.impl/link
    #'d.r.impl/package
    #'d.r.impl/template]))

(def util-bindings
  {'file-exists? #'d.r.util/file-exists?
   'os-type      #'d.r.util/os-type
   'render       #'d.r.util/render
   'load-file    "DUMMY: associated at `read-tasks` formally"
   'doc          "DUMMY: associated at `read-tasks` formally"})

(def ^:private system-binding
  {'getenv #(System/getenv %)})

(defn- build-task-bindings
  [tasks-atom]
  (letfn [(add-tasks
            [tasks]
            (doseq [task tasks]
              (d.log/debug "Read task" task)
              (swap! tasks-atom conj task)))]
    (reduce-kv
     (fn [accm fname f]
       (assoc accm fname (with-meta (comp add-tasks f) (meta f))))
     {} task-configs)))

(defn build-bindings
  [tasks]
  (-> (build-task-bindings tasks)
      (merge util-bindings)))

(defn read-tasks
  [config code-str]
  (let [tasks (atom [])
        env (or (:env config) (atom {}))
        ctx {:namespaces {'babashka.pods {'load-pod (constantly nil)}
                          d.const/pod-name (build-bindings tasks)
                          'System system-binding}
             :env env}
        ctx (-> ctx
                (assoc-in [:namespaces d.const/pod-name 'load-file] (partial d.r.util/load-file* ctx))
                (assoc-in [:namespaces d.const/pod-name 'doc] (partial d.r.util/doc (merge task-configs util-bindings) true)))
        res (sci/binding [sci/out *out*]
              (sci/eval-string code-str ctx))]
    {:res res
     :tasks (d.util/distinct-by :id @tasks)}))
