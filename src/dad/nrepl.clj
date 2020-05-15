(ns dad.nrepl
  (:require
   [babashka.nrepl.server :as nrepl]
   [dad.reader :as d.reader]
   [dad.runner :as d.runner]
   [sci.addons :as addons]
   [sci.core :as sci]))

(defn wrap-runner
  [config tasks-atom f]
  (with-meta
   (fn [& args]
     (try
       (let [ret (apply f args)]
         (when (seq @tasks-atom)
           (let [out (with-out-str
                       (d.runner/dry-run-tasks config @tasks-atom))]
             (sci/eval-string (format "(println %s)" (pr-str out)))))
         ret)
       (finally
         (reset! tasks-atom []))))
    (meta f)))

(defn wrap-doc
  [f]
  (with-meta
   (fn [& args]
     (let [out (with-out-str (apply f args))]
       (sci/eval-string (format "(println %s)" (pr-str out)))))
    (meta f)))

(defn build-namespaces
  [config]
  (let [tasks (atom [])
        wrap (partial wrap-runner config tasks)]
    (reduce
     (fn [acc [sym f]]
       (let [f (cond-> f
                 (#{'help 'dad/doc} sym) wrap-doc)]
         (if (qualified-symbol? sym)
           (assoc-in acc [(symbol (namespace sym)) (symbol (name sym))] (wrap f))
           (assoc-in acc ['user sym] (wrap f)))))
     {} (d.reader/build-bindings tasks config))))

(defn build-sci-context
  [config]
  (let [opts (-> {:namespaces (build-namespaces config)}
                 ;; c.f. https://github.com/babashka/babashka.nrepl#complaints-about-clojuremainrepl-requires
                 (assoc-in [:namespaces 'clojure.main 'repl-requires]
                           '[[clojure.repl :refer [dir doc]]])
                 addons/future)
        ctx (sci/init opts)]
    ctx))

(defn start-server
  [config]
  (let [config (assoc-in config [:log :compact?] true)
        opt (if-let [port (get-in config [:nrepl :port])]
              {:port port}
              {})
        context (build-sci-context config)]
    (nrepl/start-server! context opt)))
