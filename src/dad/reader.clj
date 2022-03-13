(ns dad.reader
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [dad.const :as d.const]
   [dad.logger :as d.log]
   [dad.os :as d.os]
   [dad.reader.impl :as d.r.impl]
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

(defn- extract-function-input-schema
  [v]
  (let [m (meta v)]
    (-> (get m :schema
             (get m :malli/schema))
        (m/schema)
        (m/-function-info)
        (get :input))))

(defn wrap-task
  [function-var]
  (with-meta (fn [& args]
               (let [schema (some-> function-var
                                    (extract-function-input-schema))]
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
  {'file-exists? #(some-> % io/file (.exists))
   'os-type      (fn [] (name (d.os/os-type)))
   'render       #(d.util/expand-map-to-str %1 %2 "{{" "}}")
   'load-file    "DUMMY: associated at `read-tasks` formally"})

(def ^:private system-binding
  {'getenv #(System/getenv %)})

(defn- extract-doc
  [resource-name]
  (some->> (io/resource "docs.adoc")
           (io/reader)
           (line-seq)
           (drop-while #(not= (str "= " resource-name) %))
           seq
           (drop 2)
           (take-while #(not= "// }}}" %))
           (str/join "\n")
           (str/trim)))

(defn doc
  ([]
   (println "### Built-in vars/functions")
   (doseq [x (keys util-bindings)]
     (println (str "* " x)))
   (println "")
   (println "### Resources")
   (doseq [x (keys task-configs)]
     (println (str "* " x)))
   (println "\nTo see detailed document: (dad/doc \"name\")"))
  ([resource-name]
   (if-let [s (extract-doc (str resource-name))]
     (println s)
     (println (str "Unknown name: " resource-name)))))

(defn load-file*
  [ctx path]
  (-> path
      (slurp)
      (sci/eval-string ctx)))

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
      (merge util-bindings)
      (assoc 'doc doc
             ;; Alias for easy to remember
             'help doc)))

(defn read-tasks
  [config code-str]
  (let [tasks (atom [])
        env (:env config (atom {}))
        ctx {:namespaces {'babashka.pods {'load-pod (constantly nil)}
                          d.const/pod-name (build-bindings tasks)
                          'System system-binding}
             :env env}
        ctx (assoc-in ctx [:namespaces d.const/pod-name 'load-file] (partial load-file* ctx))
        res (sci/binding [sci/out *out*]
              (sci/eval-string code-str ctx))]
    {:res res
     :tasks (d.util/distinct-by :id @tasks)}))
