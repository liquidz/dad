(ns dad.reader.impl
  (:require [clojure.string :as str]
            [dad.util :as d.util]))

(defn expand
  ([m]
   (reduce-kv (fn [res k v]
                (assoc res k (d.util/expand-map-to-str v m "{{" "}}")))
              {} m))
  ([s m]
   (d.util/expand-map-to-str s m "{{" "}}")))

(defn directory [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :directory
             :action (keyword action)
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn execute [m]
  (let [{:keys [cwd command pre pre-not]} m
        command (cond->> command
                  (sequential? command) (str/join "\n"))]
    (cond-> {:type :execute
             :command command
             :cwd cwd}
      pre (assoc :pre pre)
      pre-not (assoc :pre-not pre-not))))

(defn file [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :file
             :path path
             :action (keyword action)}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn git [m]
  (let [{:keys [path url revision mode owner group] :or {revision "master"}} m]
    (cond-> {:type :git
             :url url
             :path path
             :revision revision}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn download [m]
  (let [{:keys [path url mode owner group]} m]
    (cond-> {:type :download
             :url url
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn link [m]
  (let [{:keys [path to]} m]
    {:type :link
     :path path
     :to to}))

(defn package [m]
  (let [{:keys [name action] :or {action :install}} m]
    (for [name (if (sequential? name) name [name])]
      {:type :package
       :name (d.util/ensure-str name)
       :action (keyword action)})))

(defn template [m]
  (let [{:keys [path source mode owner group variables]} m]
    (cond-> {:type :template
             :path path
             :source source}
      variables (assoc :variables variables)
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))
