(ns dad.reader.util
  (:require
   [clojure.java.io :as io]
   [dad.os :as d.os]
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
  name (d.os/os-type))

(defn render "Render a template string with a data
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
