(ns loja.email
  (:require [postal.core :as postal]))

(defn send-email [{:keys [domain mail-login]}
                  {:keys [to subject body]}]
  (postal/send-message
   mail-login
   {:from (str "Loja Semente <loja@" domain ">")
    :to to
    :subject subject
    :body body}))

(comment

  (require '[loja.config :as cfg])
  (send-email
   (cfg/load "dev")
   {:to "euccastro@gmail.com"
    :subject "Sabes que?"
    :body "O leite mola!"})
;; => {:code 0, :error :SUCCESS, :message "messages sent"}

  )
