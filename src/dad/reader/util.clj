(ns dad.reader.util
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [dad.os :as d.os]
   [dad.schema :as d.schema]
   [dad.util :as d.util]
   [sci.core :as sci]))

(defn file-exists?
  "Return true if the specified file-path exists

  Examples
  ```clojure
  (file-exists? \"/path/to/file\") ;; => true
  ```"
  [file-path]
  (some-> (io/file file-path)
          (.exists)))

(defn os-type
  "Return OS type

  Examples
  ```clojure
  (os-type) ;; => \"mac\"
  ```"
  []
  (name (d.os/os-type)))

(defn render
  "Render a template string with a data

  Examples
  ```clojure
  (render \"hello\" {}) ;; => \"hello\"

  (render \"hello {{msg}}\" {:msg \"world\"}) ;; => \"hello world\"

  (render \"hello {{not-found}}\" {:msg \"world\"}) ;; => \"hello {{not-found}}\"
  ```"
  [s variables]
  (d.util/expand-map-to-str s variables "{{" "}}"))

(defn load-file*
  "Load another recipe file

  Examples
  ```clojure
  (load-file \"foo.clj\")
  ```"
  [ctx path]
  (-> path
      (slurp)
      (sci/eval-string ctx)))

(defn- gen-doc
  [var-dict sym]
  (when-let [v (get var-dict sym)]
    (let [schema-doc (try
                       (some-> (d.schema/extract-function-input-schema v)
                               (d.schema/function-schema->docstring))
                       (catch Exception _ nil))
          doc (:doc (meta v))]
      (if (and doc schema-doc)
        (str/replace-first doc "{{schema}}" schema-doc)
        doc))))

(defn doc
  "Print document

  Examples
  ```clojure
  (doc \"directory\")
  ```"
  [var-dict print? sym]
  (let [sym (if (string? sym) (symbol sym) sym)
        docstr (gen-doc var-dict sym)]
    (cond
      (and print? docstr)
      (println docstr)

      (and print? (nil? docstr))
      (println (str "Document not found: " sym))

      :else
      docstr)))
