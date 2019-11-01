(ns oton.os
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def os-name
  (str/lower-case (System/getProperty "os.name")))

(defn- linux-type []
  (let [e? #(.exists (io/file %))]
    (cond
      (e? "/etc/lsb-release") ::ubuntu
      (e? "/etc/debian_release") ::debian
      :else ::unknown-linux)))

(def os-type
  (condp #(str/includes? %2 %1) os-name
    "linux" (linux-type)
    "mac" ::mac
    "bsd" ::bsd
    ::unknown))
