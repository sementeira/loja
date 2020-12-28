(ns loja.ops
  (:require [loja.db-model.shopkeeper :as db-sk]
            [loja.crypto :as crypto]
            [loja.email :as email]))

(def ^:private msg
  "Olá,

Para criar a tua senha clica no seguinte endereço:

")

(defn add-shopkeeper [system display-name email]
  (if-let [eid (db-sk/add-shopkeeper system display-name email)]
    (email/send-email
     system
     {:to email
      :subject "Invitaçom para atender a loja da Semente"
      :body (str
             msg
             "http://localhost:9500/senha/"
             (crypto/urlsafe-encrypt
              system
              [:set-password
               {:id eid
                :t (System/currentTimeMillis)}]))})
    (throw (ex-info "could not transact shopkeeper" {}))))
