(ns oton.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [oton.os :as o.os]))

(defn read-config
  ([]
   (read-config (keyword (name o.os/os-type))))
  ([profile]
   (read-config (io/resource "config.edn") profile))
  ([config-file profile]
   (aero/read-config config-file {:profile profile})))
