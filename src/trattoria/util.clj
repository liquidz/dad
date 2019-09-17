(ns trattoria.util)

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
