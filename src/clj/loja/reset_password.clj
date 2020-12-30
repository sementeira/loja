(ns loja.reset-password
  (:require
   [loja.crypto :as crypto]))

(def one-day-ms (* 1000 60 60 24))

(def expiration-days 3)

(def ^:private msg
  (format
   "Olá,

Para estabelecer a tua senha clica no seguinte endereço (caduca em %s dias):

"
   expiration-days))


(defn reset-password-body
  [callback-host eid password]
  (str
   msg
   callback-host
   "/cb/"
   (crypto/urlsafe-encrypt
    [:set-password
     {:id eid
      :expiration (+ (System/currentTimeMillis)
                     (* one-day-ms expiration-days))}]
    password)))
