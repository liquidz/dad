(ns daddy.test-helper
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [daddy.config :as d.config]
            [daddy.logger :as d.log]
            [daddy.os :as d.os]))

(def test-os-type ::testing)

(defn read-test-config []
  (d.config/read-config (io/resource "test_config.edn") :test))

(defmacro with-test-sh [success? & body]
  `(with-redefs [d.os/os-name (fn [] ~test-os-type)
                 sh/sh (fn [& args#]
                         {:exit (if ~success? 0 1) :args args#})]
     (binding [d.log/*level* :silent]
       ~@body)))
