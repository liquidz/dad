(ns dad.util
  (:require
   [clojure.string :as str])
  (:import
   java.security.MessageDigest))

(defn distinct-by
  [f coll]
  (loop [[first-elm & rest-elms] coll
         ids #{}
         result []]
    (if-not first-elm
      result
      (let [id (f first-elm)]
        (if (contains? ids id)
          (recur rest-elms ids result)
          (recur rest-elms (conj ids id) (conj result first-elm)))))))

(defn ensure-str
  [x]
  (if (keyword? x)
    (name x)
    (str x)))

(defn ensure-seq
  [x]
  (cond-> x
    (not (sequential? x)) vector))

(defn expand-map-to-str
  ([s m] (expand-map-to-str s m "%" "%"))
  ([s m start end]
   (reduce-kv
    (fn [res k v]
      (str/replace res (str start (name k) end) (str v)))
    s m)))

(defn sha256
  [^String s]
  (->> (.getBytes s "UTF-8")
       (.digest (MessageDigest/getInstance "SHA-256"))
       (map (partial format "%02x"))
       (apply str)))

(defmacro with-out-str-and-ret
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*out* s#]
       (let [ret# (do ~@body)
             out# (str s#)]
         {:ret ret#
          :out (when (seq out#) out#)}))))
