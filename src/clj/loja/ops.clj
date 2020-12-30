(ns loja.ops
  (:require [loja.db-model.shopkeeper :as db-sk]
            [loja.email :as email]
            [loja.reset-password :as rp]))

(defn add-shopkeeper [{:keys [callback-host
                              password]
                       :as system}
                      display-name
                      email]
  (if-let [eid (db-sk/add-shopkeeper system display-name email)]
    (email/send-email
     system
     {:to email
      :subject "Invitaçom para atender a loja da Semente"
      :body (rp/reset-password-body
             callback-host
             eid
             password)})
    (throw (ex-info "could not transact shopkeeper" {}))))

(comment
  (require '[loja.config :as config])
  (require '[crux.api :as crux])
  (require '[loja.system :as system])

  (def node (:crux-node system/system))
  ;; or
  (def node (crux/start-node {}))

  (add-shopkeeper (assoc (config/load "dev")
                         :crux-node node)
                  "Manolo Peres"
                  "euccastro@gmail.com")
  (.close node)
  )
