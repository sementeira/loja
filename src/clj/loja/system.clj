(ns loja.system
  (:require
   [loja.crux :as crux]
   [loja.web :as web]
   [ring.middleware.session.cookie :refer [cookie-store]]))

(defn start [{:keys [crux-dir
                     dev-http-handler?
                     http-port
                     dev-scenarios]
              :as config}]
  (let [crux-node (crux/crux-node crux-dir)
        handler ((if dev-http-handler? web/dev-handler web/handler)
                 (assoc config
                        :crux-node crux-node
                        :session-store (cookie-store)))
        http-server (web/start-server http-port handler)]
    (reduce
     (fn [acc f+args]
       (let [[f & args] (if (seqable? f+args)
                          f+args
                          [f+args])]
         (apply
          (requiring-resolve
           (symbol
            "loja.dev-scenarios"
            (name f)))
          acc
          args)))
     {:crux-node crux-node
      :http-server http-server}
     dev-scenarios)))

(defn stop [{:keys [crux-node http-server]}]
  (web/stop-server http-server)
  (crux/close crux-node))

(comment
  (require '[loja.config :as config])
  (def system (start (config/load "dev")))
  (stop system)
  )
