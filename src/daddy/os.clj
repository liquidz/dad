(ns daddy.os
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn os-name []
  (str/lower-case (System/getProperty "os.name")))

(defn- linux-type []
  (let [e? #(.exists (io/file %))]
    (cond
      (e? "/etc/lsb-release") ::ubuntu
      (e? "/etc/debian_release") ::debian
      (e? "/etc/arch-release") ::archlinux
      :else ::unknown-linux)))

(defn os-type []
  (condp #(str/includes? %2 %1) (os-name)
    "linux" (linux-type)
    "mac" ::mac
    "bsd" ::bsd
    ::unknown))
