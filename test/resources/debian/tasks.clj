(let [base-dir "/tmp/foo/bar"]
  (directory base-dir)
  (file (str base-dir "/baz")))

(git {:url "https://github.com/liquidz/trattoria"
      :path "/tmp/trattoria"})

(package "make")
