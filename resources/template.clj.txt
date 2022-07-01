(ns %s
  (:require
   [babashka.pods :as pods]))

(pods/load-pod "dad")
(require '[pod.liquidz.dad :as dad])

(comment (dad/directory {:path "foo"}))
