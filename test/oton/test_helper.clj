(ns oton.test-helper
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [oton.config :as o.config]
            [oton.os :as o.os]))

(def test-os-type ::testing)

(defn read-test-config []
  (o.config/read-config (io/resource "test_config.edn") :test))

(defmacro with-test-sh [success? & body]
  `(with-redefs [o.os/os-name ~test-os-type
                 sh/sh (fn [& args#]
                         {:exit (if ~success? 0 1) :args args#})]
     ~@body))
