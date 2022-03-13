(require '[pod.liquidz.dad :as dad])

(dad/package {:name "sl" :action :uninstall}) ;; should be skipped
(dad/package {:name "sl"})
(dad/package {:name "cowsay" :action :uninstall})

(let [opt {:path (base-dir "tmpl")
           :source "template.tmpl"
           :variables {:foo "bar" :bar "baz"}
           :mode "644"}]
  (dad/template opt)
  (dad/template (assoc opt :mode "755")))
