(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def ^:private class-dir "target/classes")
(def ^:private main 'dad.core)
(def ^:private uber-file "target/dad.jar")

(defn uberjar
  [_]
  (let [basis (b/create-basis)]
    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    :src-dirs ["src"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main main})))
