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
(def ^:private ?path-actions
  [:enum
   :create :delete :remove
   "create" "delete" "remove"])

(defn directory
  "Create directories

{{schema}}

  Examples
  ```clojure
  (directory {:path \"foo/bar\"})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:action {:optional true :default :create} ?path-actions]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [{:keys [path action mode owner group]}]
  (let [action (or action :create)]
    (cond-> {:type :directory
             :action (keyword action)
             :path path}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn execute
  "Execute a shell command.

{{schema}}

  Examples
  ```clojure
  (execute {:cwd \"/tmp\" :command \"curl -sfLo foobar https://example.com\"})

  (execute {:command \"touch foo\" :pre-not \"test -e bar\"})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:command [:or
                                        ?non-blank-string
                                        [:vector ?non-blank-string]]]
                             [:cwd {:optional true} ?non-blank-string]
                             [:pre {:optional true} ?non-blank-string]
                             [:pre-not {:optional true} ?non-blank-string]]]
                  ?task]}
  [{:keys [command cwd pre pre-not]}]
  (let [command (cond->> command
                  (sequential? command) (str/join "\n"))]
    (cond-> {:type :execute
             :command command
             :cwd cwd}
      pre (assoc :pre pre)
      pre-not (assoc :pre-not pre-not))))

(defn file
  "Create a file.

{{schema}}

  Examples
  ```clojure
  (file {:path \"foobar\" :mode \"755\"})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:action {:optional true :default :create} ?path-actions]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [{:keys [path action mode owner group]}]
  (let [action (or action :create)]
    (cond-> {:type :file
             :path path
             :action (keyword action)}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn git
  "Execute `git` command.

{{schema}}

  Examples
  ```clojure
  (git {:path \"dad-source\" :url \"https://github.com/liquidz/dad\"})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:url ?non-blank-string]
                             [:revision {:optional true :default "main"} ?non-blank-string]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [{:keys [path url revision mode owner group]}]
  (let [revision (or revision "main")]
    (cond-> {:type :git
             :url url
             :path path
             :revision revision}
      mode (assoc :mode mode)
      owner (assoc :owner owner)
      group (assoc :group group))))

(defn download
  "Download a file from remote host.
  This resource requires `curl` command.

{{schema}}

  Examples
  ```clojure
  (download {:path \"foobar\" :url \"https://example.com\"})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:url ?non-blank-string]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [{:keys [path url mode owner group]}]
  (cond-> {:type :download
           :url url
           :path path}
    mode (assoc :mode mode)
    owner (assoc :owner owner)
    group (assoc :group group)))

(defn link
  "Create a symbolic link.

{{schema}}

  Examples
  ```clojure
  (link {:path \"~/.lein/profiles.clj\" :source \"/path/to/your/dotfiles/profiles.clj\"})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:source ?non-blank-string]]]
                  ?task]}
  [{:keys [path source]}]
  {:type :link
   :path path
   :source source})

(defn package
  "Install packages.

{{schema}}

  Examples
  ```clojure
  (package {:name \"vim\"})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:name [:or
                                     ?non-blank-string
                                     [:vector ?non-blank-string]]]
                             [:action {:optional true
                                       :default :install}
                              [:enum
                               :install :remove :delete :uninstall
                               "install" "remove" "delete" "uninstall"]]]]
                  ?task]}
  [{:keys [name action]}]
  (let [action (or action :install)]
    (for [name (if (sequential? name) name [name])]
      {:type :package
       :name (d.util/ensure-str name)
       :action (keyword action)})))

(defn template
  "Create a text file from the specified template files.

{{schema}}

  Examples
  ```clojure
  (template {:path \"result.txt\" :source \"source.txt\" :variables {:msg \"world\"}})
  ```"
  {:malli/schema [:=> [:cat [:map
                             [:path ?non-blank-string]
                             [:source ?non-blank-string]
                             [:variables {:optional true} 'map?]
                             [:mode {:optional true} ?non-blank-string]
                             [:owner {:optional true} ?non-blank-string]
                             [:group {:optional true} ?non-blank-string]]]
                  ?task]}
  [{:keys [path source mode owner group variables]}]
  (cond-> {:type :template
           :path path
           :source source}
    variables (assoc :variables variables)
    mode (assoc :mode mode)
    owner (assoc :owner owner)
    group (assoc :group group)))
