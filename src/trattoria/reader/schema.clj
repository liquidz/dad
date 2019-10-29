(ns trattoria.reader.schema
  (:require [malli.core :as m]
            [malli.error :as me]))

(def path-action
  [:map
   [:action {:optional true} [:enum :create :delete :remove]]])

(def path-mode
  [:map
   [:mode {:optional true} string?]
   [:owner {:optional true} string?]
   [:group {:optional true} string?]])

(def directory*
  (m/merge
   path-action
   path-mode
   [:map [:path string?]]))

(def execute*
  [:map
   [:command [:or string? [:vector string?]]]
   [:cwd {:optional true} string?]])

(def file*
  (m/merge
   path-action
   path-mode
   [:map [:path string?]]))

(def git*
  [:map
   [:path string?]
   [:url string?]
   [:revision {:optional true} string?]])

(def link*
  [:map
   [:path string?]
   [:to string?]])

(def package*
  [:map
   [:name [:or string? [:vector string?]]]
   [:action {:optional true} [:enum :install :remove :uninstall]]])

(def template*
  (m/merge
   path-mode
   [:map
    [:path string?]
    [:source string?]
    [:variables {:optional true} map?]]))

(defn validate [value schema]
  (if-let [err (some-> schema
                       (m/explain value)
                       me/humanize)]
    (throw (ex-info "validation error" err))
    value))
