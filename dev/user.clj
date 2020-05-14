(ns user
  (:require
   [babashka.nrepl.server :as nrepl]
   [dad.config :as d.config]
   [dad.nrepl :as d.nrepl]))

(defonce server (atom nil))

(defn start
  []
  (when-not @server
    (println "Starting nREPL server")
    (reset! server (d.nrepl/start-server (d.config/read-config)))))
(defn stop
  []
  (when @server
    (println "Stopping nREPL server")
    (nrepl/stop-server! @server)
    (reset! server nil)))

(defn go
  []
  (stop)
  (start))
