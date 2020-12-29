(ns loja.system
  (:require
   [loja.crux :as crux]
   [loja.web :as web]))

(defn start [{:keys [crux-dir http-port]}]
  (let [crux-node (crux/crux-node crux-dir)
        handler (web/handler crux-node)
        http-server (web/start-server http-port handler)]
    {:crux-node crux-node
     :http-server http-server}))

(defn stop [{:keys [crux-node http-server]}]
  (web/stop-server http-server)
  (crux/close crux-node))

(comment
  (require '[loja.config :as config])
  (def system (start (config/load "dev")))
  (stop system)
  )
