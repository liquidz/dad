(ns daddy.logger
  (:require [clojure.string :as str]))

(def ^:dynamic *level* :info)
(def ^:private levels (zipmap [:debug :info :warn :error :silent] (range)) )
(def ^:private color-codes
  {:debug 97
   :info 32
   :warn 35
   :error 31
   :silent 30})

(defn- colorize [code s]
  (str \u001b "[" code "m" s \u001b "[m"))

(defn- log [level msg & more]
  (when (<= (get levels *level*) (get levels level))
    (let [colorize* (partial colorize (get color-codes level :debug))]
      (apply println
             (cond-> [(colorize* (str/upper-case (name level)))
                       ":"
                       msg]
               more (concat more))))
    nil))

(def debug (partial log :debug))
(def info (partial log :info))
(def warn (partial log :warn))
(def error (partial log :error))
