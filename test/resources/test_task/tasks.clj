#!/usr/bin/env dad --no-color --dry-run
(ns foo
  (:require
   [pod.liquidz.dad :as dad]))

(defn base-dir [x]
  (dad/render "/tmp/dad_test/{{x}}" {:x  x}))

(dad/directory {:path (base-dir "post") :mode "755"})
(dad/directory {:path (base-dir "pre/a") :action :delete})
(dad/directory {:path (base-dir "post") :mode "744"})

(dad/download {:path (base-dir "project.clj")
               :url "https://raw.githubusercontent.com/liquidz/dad/master/project.clj"
               :mode "755"})

(dad/file {:path (base-dir "foo")})
(dad/file {:path (base-dir "pre/dummy") :action :delete})

(dad/git {:url "https://github.com/liquidz/dad"
          :path (base-dir "dad")})

(dad/execute {:command "touch hello"
              :cwd (base-dir "")})

(dad/execute {:command "touch error"
              :cwd (base-dir "")
              :pre-not "test -e hello"})

(dad/link {:path (base-dir "world")
           :to (base-dir "hello")})

(dad/load-file "loaded_tasks.clj")
