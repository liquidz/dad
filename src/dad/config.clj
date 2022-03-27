(ns dad.config
  (:require
   [aero.core :as aero]
   [clojure.java.io :as io]
   [dad.os :as d.os]))

(defn read-config
  ([]
   (read-config (keyword (name (d.os/os-type)))))
  ([profile]
   (read-config (io/resource "config.edn") profile))
  ([config-file profile]
   (aero/read-config config-file {:profile profile})))
