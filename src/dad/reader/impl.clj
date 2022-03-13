(ns dad.reader.impl
  (:require
   [clojure.string :as str]
   [dad.util :as d.util]))

(def ^:private ?task
  [:map [:type :keyword]])
(def ^:private ?non-blank-string
  [:and
   'string?
   [:not= {:error/message "should not be blank"} ""]])

(defn directory
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:action {:optional true} ?non-blank-string]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :directory
             :action (keyword action)
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn execute
  {:malli/schema [:=> [:cat [:map
                             [:comand [:or
                                       ?non-blank-string
                                       [:vector ?non-blank-string]]]
                             [:cwd {:optional true} ?non-blank-string]
                             [:pre {:optional true} ?non-blank-string]
                             [:pre-not {:optional true} ?non-blank-string]]]
                  ?task]}
  [m]
  (let [{:keys [cwd command pre pre-not]} m
        command (cond->> command
                  (sequential? command) (str/join "\n"))]
    (cond-> {:type :execute
             :command command
             :cwd cwd}
      pre (assoc :pre pre)
      pre-not (assoc :pre-not pre-not))))

(defn file
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:action {:optional true} ?non-blank-string]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [m]
  (let [{:keys [path action mode owner group] :or {action :create}} m]
    (cond-> {:type :file
             :path path
             :action (keyword action)}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn git
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:url ?non-blank-string]
                             [:revision {:optional true} ?non-blank-string]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [m]
  (let [{:keys [path url revision mode owner group] :or {revision "master"}} m]
    (cond-> {:type :git
             :url url
             :path path
             :revision revision}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn download
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:url ?non-blank-string]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [m]
  (let [{:keys [path url mode owner group]} m]
    (cond-> {:type :download
             :url url
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn link
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:to ?non-blank-string]]]
                  ?task]}
  [m]
  (let [{:keys [path to]} m]
    {:type :link
     :path path
     :to to}))

(defn package
  {:malli/schema [:=> [:cat [:map
                             [:name [:or
                                     ?non-blank-string
                                     [:vector ?non-blank-string]]]
                             [:action {:optional true}
                              [:enum
                               :install :remove :delete :uninstall
                               "install" "remove" "delete" "uninstall"]]]]

                  ?task]}
  [m]
  (let [{:keys [name action] :or {action :install}} m]
    (for [name (if (sequential? name) name [name])]
      {:type :package
       :name (d.util/ensure-str name)
       :action (keyword action)})))

(defn template
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:source ?non-blank-string]
                             [:variables {:optional true} 'map?]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [m]
  (let [{:keys [path source mode owner group variables]} m]
    (cond-> {:type :template
             :path path
             :source source}
      variables (assoc :variables variables)
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))
