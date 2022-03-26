#!/usr/bin/env dad --no-color --dry-run
(ns example.clojure.cli
  (:require
   [babashka.pods :as pods]))

;; Load dad as a babashka pod
(pods/load-pod "dad")
(require '[pod.liquidz.dad :as dad])

(dad/package {:name ["curl" "rlwrap"]})

;; You can define function as you like
(defn curl [m]
  (let [{:keys [path url]} m]
    ;; `file-exists?` is a built-in function in dad.
    (when (and (not (dad/file-exists? path))
               (string? url))
      (dad/execute {:command (str "curl -sfLo " path " " url)}))))

(curl {:path "/tmp/install.sh"
       :url "https://download.clojure.org/install/linux-install-1.10.3.1087.sh"})

(dad/file {:path "/tmp/install.sh" :mode "755"})
(dad/execute {:cwd "/tmp" :command "./install.sh"})
(dad/file {:path "/tmp/install.sh" :action :delete})
