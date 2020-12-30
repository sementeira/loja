(ns loja.reset-password
  (:require
   [loja.crypto :as crypto]
   [loja.db-model.shopkeeper :as db-sk]
   [loja.email :as email]
   [loja.layout :refer [html5-ok]]
   [ring.util.http-response :refer [see-other]]
   [buddy.hashers :as hashers]))

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

(defn add-shopkeeper
  "To call from the REPL"
  [{:keys [callback-host
           crux-node
           password]
    :as system}
   display-name
   email]
  (if-let [eid (db-sk/add-shopkeeper crux-node display-name email)]
    (email/send-email
     system
     {:to email
      :subject "Invitaçom para atender a loja da Semente"
      :body (reset-password-body
             callback-host
             eid
             password)})
    (throw (ex-info "could not transact shopkeeper" {}))))

(defn handle-callback [_ password payload]
  (let [[op {:keys [expiration]}]
        (crypto/decrypt-urlsafe payload password)]
    (assert (= op :set-password))
    (if (< expiration (System/currentTimeMillis))
      (html5-ok "Ligaçom caducada"
                [[:h1 "Ligaçom caducada"]
                 [:p "Passou o dia, passou a romaria."]])
      (html5-ok "Estabelece a tua senha"
                [[:h1 "Estabelece a tua senha"]
                 [:form {:method :post
                         :action "/estabelece-senha"}
                  [:input#payload {:type :hidden
                                   :name "payload"
                                   :value payload}]
                  [:div
                   [:label {:for "pass1"} "Senha"]
                   [:input#pass1 {:type :text
                                  :name "pass1"}]]
                  [:div
                   [:label {:for "pass2"} "Confirma senha"]
                   [:input#pass2 {:type :text
                                  :name "pass2"}]]
                  [:div
                   [:input {:type :submit :value "Enviar"}]]]]))))

(defn reset-password [crux-node password payload pass1 pass2]
  (let [[op {:keys [id expiration]}]
        (crypto/decrypt-urlsafe payload password)]
    (assert (= op :set-password))
    (cond
      (< expiration (System/currentTimeMillis))
      ;; XXX: redirect
      (html5-ok "Ligaçom caducada"
                [[:h1 "Ligaçom caducada"]
                 [:p "Passou o dia, passou a romaria."]])
      (not= pass1 pass2)
      ;; XXX: redirect
      (html5-ok "As senhas nom quadram"
                [[:h1 "As senhas nom quadram"]
                 [:p "Tenta de novo (XXX ligaçom)"]])
      :else
      (do
        (assert (db-sk/set-password crux-node id pass1))
        ;; XXX: redirect
        (html5-ok "Agora estabeleceria-che a senha"
                  [[:h1 "Tudo bem"]
                   [:p "Agora restabeleceria-che a senha"]])))))
(comment
  (do
    (require '[loja.config :as config])
    (require '[buddy.hashers :as hashers])
    (require '[crux.api :as crux])
    (require '[loja.crux :as lcrux])
    (require '[loja.system :as system]))

  (def node (:crux-node system/system))
  ;; or
  (def node (crux/start-node {}))

  ()
  (hashers/check
   "senha"
   (lcrux/q1 node '{:find [pass]
                    :where [[_ :loja.shopkeeper/password pass]]}))

  (add-shopkeeper (assoc (config/load "dev")
                         :crux-node node)
                  "Manolo Peres"
                  "euccastro@gmail.com")

  (.close node)
  )
