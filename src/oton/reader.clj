(ns oton.reader
  (:require [camel-snake-kebab.core :as csk]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [oton.os :as o.os]
            [oton.reader.schema :as o.r.schema]
            [oton.util :as o.util]
            [sci.core :as sci]))

(defn- task-id [m]
  (->> m
       (sort-by (comp str first))
       (map (comp str second))
       (str/join "")))

(defn- directory* [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :directory
             :action action
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn- execute* [m]
  (let [{:keys [cwd command]} m
        command (cond->> command
                  (sequential? command) (str/join "\n"))]
    {:type :execute
     :command command
     :cwd cwd}))

(defn- file* [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :file :path path :action action}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn- git* [m]
  (let [{:keys [path url revision] :or {revision "master"}} m]
    {:type :git
     :url url
     :path path
     :revision revision}))

(defn- link* [m]
  (let [{:keys [path to]} m]
    {:type :link
     :path path
     :to to}))

(defn- package* [m]
  (let [{:keys [name action] :or {action :install}} m]
    (for [name (if (sequential? name) name [name])]
      {:type :package
       :name name
       :action action})))

(defn- template* [m]
  (let [{:keys [path source mode owner group variables]} m]
    (cond-> {:type :template
             :path path
             :source source}
      variables (assoc :variables variables)
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn- dispatch [config & args]
  (let [{:keys [destination resource-name-key schema]} config
        [resource-name m] (cond->> args
                            (some-> args first map?) (cons nil))
        arg-map (when (or (nil? m) (map? m))
                  (cond-> (or m {})
                    resource-name (assoc resource-name-key resource-name)))
        arg-map (o.r.schema/validate arg-map schema)]
    (destination arg-map)))

(def ^:private task-configs
  {'directory {:destination directory*
               :resource-name-key :path
               :schema o.r.schema/directory*}
   'execute   {:destination execute*
               :resource-name-key :command
               :schema o.r.schema/execute*}
   'file      {:destination file*
               :resource-name-key :path
               :schema o.r.schema/file*}
   'git       {:destination git*
               :resource-name-key :path
               :schema o.r.schema/git*}
   'link      {:destination link*
               :resource-name-key :path
               :schema o.r.schema/link*}
   'package   {:destination package*
               :resource-name-key :name
               :schema o.r.schema/package*}
   'template  {:destination template*
               :resource-name-key :path
               :schema o.r.schema/template*}})

(def ^:private util-bindings
  {
   ; '$env (reduce (fn [res [k v]] (assoc res
   ;                                      (keyword k) v
   ;                                      (csk/->kebab-case-keyword k) v))
   ;               {} (System/getenv))
   'env #(get (System/getenv) (csk/->SCREAMING_SNAKE_CASE_STRING %))
   'exists? #(some-> % io/file (.exists))
   'os-type (name o.os/os-type)
   'println println
   'str/join str/join})



(defn read-tasks [code-str]
  (let [tasks (atom [])
        add-task (fn [res]
                   (when res
                     (let [x (->> (if (sequential? res) res [res])
                                  (map #(assoc % :id (task-id %))))]
                       (swap! tasks #(vec (concat % x))))))
        bindings (-> (reduce-kv (fn [res k v]
                                  (assoc res k (comp add-task (partial dispatch v))))
                                {} task-configs)
                     (merge util-bindings))]
    (doall (sci/eval-string code-str {:bindings bindings}))
    (o.util/distinct-by :id @tasks)))
