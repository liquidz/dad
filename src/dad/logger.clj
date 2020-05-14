(ns dad.logger
  (:require
   [clojure.string :as str]))

(def ^:dynamic *level* :info)
(def ^:dynamic *color* true)
(def ^:private levels (zipmap [:debug :info :warn :error :silent] (range)))

(def ^:private color-codes
  {:white 97
   :green 32
   :purple 35
   :red 31
   :black 30})

(def ^:private log-colors
  {:debug :white
   :info :green
   :warn :purple
   :error :red
   :silent :black})

(defn colorize
  [color-key s]
  (if *color*
    (str \u001b "[" (get color-codes color-key) "m" s \u001b "[m")
    s))

(defn- log*
  [level msg]
  (when (<= (get levels *level*) (get levels level))
    (print msg)
    (flush)
    nil))

(defn message
  [level msg & more]
  (let [colorize* (partial colorize (get log-colors level :white))
        messages (cond-> [(colorize* (str/upper-case (name level)))
                          ":"
                          msg]
                   more (concat more))]
    (str/join " " messages)))

(defn- log
  [level msg & more]
  (log* level (str (apply message level msg more) "\n")))

(def debug* (partial log* :debug))
(def info* (partial log* :info))
(def warn* (partial log* :warn))
(def error* (partial log* :error))

(def debug (partial log :debug))
(def info (partial log :info))
(def warn (partial log :warn))
(def error (partial log :error))
