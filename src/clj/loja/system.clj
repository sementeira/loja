(ns loja.system
  (:require
   [loja.crux :as crux]
   [loja.web :as web]))

(defn start [{:keys [crux-dir
                     dev-http-handler?
                     http-port
                     password]}]
  (let [crux-node (crux/crux-node crux-dir)
        handler ((if dev-http-handler? web/dev-handler web/handler)
                 crux-node
                 password)
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
