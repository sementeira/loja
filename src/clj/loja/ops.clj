(ns loja.ops
  (:require [loja.db-model.shopkeeper :as db-sk]
            [loja.crypto :as crypto]
            [loja.email :as email]))

(def ^:private msg
  "Olá,

Para criar a tua senha clica no seguinte endereço:

")

(defn add-shopkeeper [{:keys [callback-host
                              callback-path
                              password]
                       :as system}
                      display-name
                      email]
  (if-let [eid (db-sk/add-shopkeeper system display-name email)]
    (email/send-email
     system
     {:to email
      :subject "Invitaçom para atender a loja da Semente"
      :body (str
             msg
             callback-host
             callback-path
             (crypto/urlsafe-encrypt
              [:set-password
               {:id eid
                :t (System/currentTimeMillis)}]
              password))})
    (throw (ex-info "could not transact shopkeeper" {}))))

(comment
  (require '[loja.config :as config])
  (require '[crux.api :as crux])
  (def node (crux/start-node {}))
  (add-shopkeeper (assoc (config/load "dev")
                         :crux-node node)
                  "Manolo Peres"
                  "euccastro@gmail.com")
  (.close node)
  )
