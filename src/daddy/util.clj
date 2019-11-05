(ns daddy.util
  (:require [clojure.string :as str]))

(defn distinct-by [f coll]
  (loop [[first-elm & rest-elms] coll
         ids #{}
         result []]
    (if-not first-elm
      result
      (let [id (f first-elm)]
        (if (contains? ids id)
          (recur rest-elms ids result)
          (recur rest-elms (conj ids id) (conj result first-elm)))))))

(defn ensure-seq [x]
  (cond-> x
    (not (sequential? x)) vector))

(defn expand-map-to-str [s m]
  (reduce-kv
   (fn [res k v]
     (str/replace res (str "%" (name k) "%") (str v)))
   s m))
