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

  (send-email {:to "euccastro@gmail.com"
               :subject "Olá mais umha vez da loja!"
               :body "Isto é o corpo do email"})

  )
