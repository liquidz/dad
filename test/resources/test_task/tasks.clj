(defn base-dir [x]
  (str "/tmp/daddy_test/" x))

(directory (base-dir "post"))
(directory (base-dir "pre/a") {:action :delete})

(file (base-dir "foo"))
(file (base-dir "pre/dummy") {:action :delete})

(git {:url "https://github.com/liquidz/daddy"
      :path (base-dir "daddy")})

(execute {:command "touch hello"
          :cwd (base-dir "")})

(execute {:command "touch error"
          :cwd (base-dir "")
          :pre-not "test -e hello"})

(link (base-dir "world") {:to (base-dir "hello")})

(load-file "loaded_tasks.clj")
