(defn base-dir [x]
  (str "/tmp/trattoria_test/" x))

(directory (base-dir "post"))
(directory (base-dir "pre/a") {:action :delete})

(file (base-dir "foo"))
(file (base-dir "pre/dummy") {:action :delete})

(git {:url "https://github.com/liquidz/trattoria"
      :path (base-dir "trattoria")})

(execute {:command "touch hello"
          :cwd (base-dir "")})

(link (base-dir "world") {:to (base-dir "hello")})

(package "clojure")
(package "git" {:action :uninstall})
