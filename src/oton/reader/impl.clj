(ns oton.reader.impl
  (:require [clojure.string :as str]))

(defn directory [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :directory
             :action (keyword action)
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn execute [m]
  (let [{:keys [cwd command]} m
        command (cond->> command
                  (sequential? command) (str/join "\n"))]
    {:type :execute
     :command command
     :cwd cwd}))

(defn file [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :file
             :path path
             :action (keyword action)}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn git [m]
  (let [{:keys [path url revision] :or {revision "master"}} m]
    {:type :git
     :url url
     :path path
     :revision revision}))

(defn link [m]
  (let [{:keys [path to]} m]
    {:type :link
     :path path
     :to to}))

(defn package [m]
  (let [{:keys [name action] :or {action :install}} m]
    (for [name (if (sequential? name) name [name])]
      {:type :package
       :name name
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
