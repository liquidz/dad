(ns dad.constant)

(def pod-name
  'pod.liquidz.dad)

(def require-refer-all-code
  (format "(require '[%s :refer :all])" pod-name))
