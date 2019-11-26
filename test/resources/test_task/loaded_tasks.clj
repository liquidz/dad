(package "sl" {:action :uninstall}) ;; should be skipped
(package "sl")
(package "cowsay" {:action :uninstall})

(template {:path (base-dir "tmpl")
           :source "template.tmpl"
           :variables {:foo "bar" :bar "baz"}})
