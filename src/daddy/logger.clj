(ns daddy.logger
  (:require [clojure.string :as str]))

(def ^:dynamic *level* :info)
(def ^:private levels (zipmap [:debug :info :warn :error :silent] (range)) )

(defn- log [level msg more]
  (when (<= (get levels *level*) (get levels level))
    (println (str (str/upper-case (name level)) ":") msg more)))

(def debug (partial log :debug))
(def info (partial log :info))
(def warn (partial log :warn))
(def error (partial log :error))
