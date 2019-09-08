(ns trattoria.reader
  (:require [sci.core :as sci]
            [trattoria.os :as t.os]
            [clojure.java.io :as io]))

(defn- directory* [path & [option]]
  {:pre [(or (nil? option) (map? option))
         (contains? #{nil :create :delete :remove} (:action option))]}
  (let [{:keys [action mode owner group] :or {action :create}} option]
    (cond-> {:type :directory
             :action action
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn- execute* [option]
  {:pre [(map? option)
         (contains? option :command)]}
  (let [{:keys [cwd command]} option]
    {:type :execute
     :command command
     :cwd cwd}))

(defn- file* [path & [option]]
  {:pre [(or (nil? option) (map? option))
         (string? path)
         (contains? #{nil :create :delete :remove} (:action option))]}
  (let [{:keys [action mode owner group] :or {action :create}} option]
    (cond-> {:type :file :path path :action action}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn- git* [option]
  {:pre [(map? option)
         (contains? option :url)
         (contains? option :path)]}
  (let [{:keys [url path revision] :or {revision "master"}} option]
    {:type :git
     :url url
     :path path
     :revision revision}))

(defn- package* [pkg-name & [option]]
  {:pre [(or (nil? option) (map? option))
         (contains? #{nil :install :remove :uninstall} (:action option))]}
  (let [{:keys [action] :or {action :install}} (or option {})]
    {:type :package
     :name pkg-name
     :action action}))

(defn- template* [path & [option]]
  {:pre [(or (nil? option) (map? option))
         (contains? option :source)
         (.exists (io/file (:source option)))]}
  (let [{:keys [source mode owner group variables]} option]
    (cond-> {:type :template
             :path path
             :source source}
      variables (assoc :variables variables)
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(def ^:private task-bindings
  {
   'directory directory*
   'execute execute*
   'file file*
   'git git*
   'package package*
   'template template*
   })

(def ^:private util-bindings
  {
   'exists? #(.exists (io/file %))
   'os-type (name t.os/os-type)
   'println println
   })

(defn read-tasks [code-str]
  (let [tasks (atom [])
        add-task #(and % (swap! tasks conj %))
        bindings (-> (reduce-kv #(assoc %1 %2 (comp add-task %3)) {} task-bindings)
                     (merge util-bindings))]
    (doall (sci/eval-string code-str {:bindings bindings}))
    @tasks))
