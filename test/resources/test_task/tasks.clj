#!/usr/bin/env dad --no-color --dry-run

(defn base-dir [x]
  (str "/tmp/dad_test/" x))

(directory (base-dir "post"))
(directory (base-dir "pre/a") {:action :delete})

(download {:path (base-dir "project.clj")
           :url "https://raw.githubusercontent.com/liquidz/dad/master/project.clj"})

(file (base-dir "foo"))
(file (base-dir "pre/dummy") {:action :delete})

(git {:url "https://github.com/liquidz/dad"
      :path (base-dir "dad")})

(execute {:command "touch hello"
          :cwd (base-dir "")})

(execute {:command "touch error"
          :cwd (base-dir "")
          :pre-not "test -e hello"})

(link (base-dir "world") {:to (base-dir "hello")})

(load-file "loaded_tasks.clj")
