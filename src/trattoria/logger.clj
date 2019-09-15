(ns trattoria.logger
  (:require [clojure.string :as str]))

(def ^:dynamic *level* :info)
(def ^:private levels (zipmap [:trace :debug :info :warn :error :fatal :silent] (range)) )

(defn- log [level msg more]
  (when (<= (get levels *level*) (get levels level))
    (println (str (str/upper-case (name level)) ":") msg more)))

(def trace (partial log :trace))
(def debug (partial log :debug))
(def info (partial log :info))
(def warn (partial log :warn))
(def error (partial log :error))
(def fatal (partial log :fatal))
