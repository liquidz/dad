(ns dad.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [dad.os :as d.os]))

(defn version
  []
  (-> "version.txt" io/resource slurp str/trim))

(defn read-config
  ([]
   (read-config (keyword (name (d.os/os-type)))))
  ([profile]
   (read-config (io/resource "config.edn") profile))
  ([config-file profile]
   (aero/read-config config-file {:profile profile})))
