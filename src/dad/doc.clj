(ns dad.doc
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [dad.reader :as d.reader]
   [dad.reader.util :as d.r.util]
   [dad.schema :as d.schema]))

(def ^:private target-vars
  (-> (merge d.reader/task-configs
             d.reader/util-bindings
             {'load-file (with-meta d.r.util/load-file*
                           (assoc (meta #'d.r.util/load-file*) :arglists '([path])))
              'doc (with-meta d.r.util/doc
                     (assoc (meta #'d.r.util/doc) :arglists '([] [sym])))})
      (vals)))

(def ^:private rename-dict
  {'load-file* 'load-file
   'file-exists? 'file-exists})

(defn- gen-document-markdown-content
  [v]
  (let [m (meta v)
        table (some-> v
                      (d.schema/extract-function-input-schema)
                      (d.schema/function-schema->markdown-table))
        table (or table "")
        doc (->> (str/split (or (:doc m) "") #"\r?\n")
                 (map #(str/replace-first % #"^  " ""))
                 (str/join "\n"))
        content (str/replace-first doc "{{schema}}" table)]
    (->> [(format "# %s" (:name m))
          ""
          content]
         (flatten)
         (str/join "\n"))))


(defn -main
  [& _]
  (println "Start to generate docment markdowns")
  (doseq [v target-vars
          :let [m (meta v)
                var-name (:name m)
                filename (str (or (get rename-dict var-name) var-name) ".md")]]
    (println "- Generating" filename)
    (spit (io/file "doc" filename)
          (gen-document-markdown-content v)))
  (println "Finish to generate docment markdowns"))
