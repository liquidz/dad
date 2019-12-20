(package "sl" {:action :uninstall}) ;; should be skipped
(package "sl")
(package "cowsay" {:action :uninstall})

(let [opt {:path (base-dir "tmpl")
           :source "template.tmpl"
           :variables {:foo "bar" :bar "baz"}
           :mode "644"}]
  (template opt)
  (template (assoc opt :mode "755")))
