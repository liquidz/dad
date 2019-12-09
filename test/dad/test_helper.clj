(ns dad.test-helper
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [dad.config :as d.config]
            [dad.logger :as d.log]
            [dad.os :as d.os]))

(def test-os-type ::testing)

(defn read-test-config []
  (d.config/read-config (io/resource "test_config.edn") :test))

(defmacro with-test-sh [success? & body]
  `(with-redefs [d.os/os-name (fn [] ~test-os-type)
                 sh/sh (fn [& args#]
                         {:exit (if ~success? 0 1) :args args#})]
     (binding [d.log/*level* :silent]
       ~@body)))
