(ns loja.email
  (:require [loja.config :as config]
            [postal.core :as postal]))

(defn send-email [{:keys [to subject body]}]
  (let [{:keys [domain mail-login]} (config/load "dev")]
    (postal/send-message
     mail-login
     {:from (str "Loja Semente <loja@" domain ">")
      :to to
      :subject subject
      :body body})))

(comment

  (send-email {:to "euccastro@gmail.com"
               :subject "Olá mais umha vez da loja!"
               :body "Isto é o corpo do email"})

  )
