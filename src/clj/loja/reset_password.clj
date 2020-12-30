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
  [callback-host eid password dbg-expired?]
  (str
   msg
   callback-host
   "/cb/"
   (crypto/urlsafe-encrypt
    [:set-password
     {:id eid
      :expiration (+ (System/currentTimeMillis)
                     (if dbg-expired?
                       -1
                       (* one-day-ms expiration-days)))}]
    password)))

(defn send-reset-email [{:keys [callback-host
                                password]
                         :as system}
                        email
                        eid
                        dbg-expired?]
  (email/send-email
   system
   {:to email
    :subject "Invitaçom para atender a loja da Semente"
    :body (reset-password-body
           callback-host
           eid
           password
           dbg-expired?)}))

(defn add-shopkeeper
  "To call from the REPL"
  ([system display-name email]
   (add-shopkeeper system display-name email false))
  ([{:keys [callback-host
            crux-node
            password]
     :as system}
    display-name
    email
    dbg-expired?]
   (if-let [eid (db-sk/add-shopkeeper crux-node display-name email)]
     (send-reset-email system email eid dbg-expired?)
     (throw (ex-info "could not transact shopkeeper" {})))))

(def errors
  {"nom-quadram" "As senhas nom quadram"})

(defn handle-callback [password payload error]
  (let [[op {:keys [expiration]}]
        (crypto/decrypt-urlsafe payload password)]
    (assert (= op :set-password))
    (if (< expiration (System/currentTimeMillis))
      (see-other "/caducada")
      (html5-ok "Estabelece a tua senha"
                [[:h1 "Estabelece a tua senha"]
                 (when error [:div {:style "color: red"} (errors error)])
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
      (see-other "/caducada")
      (not= pass1 pass2)
      (see-other (str "/cb/" payload "?erro=nom-quadram"))
      :else
      (do
        (assert (db-sk/set-password crux-node id pass1))
        ;; XXX: log in and redirect to admin page
        (html5-ok "Tudo bem"
                  [[:h1 "Tudo bem"]
                   [:p "Agora redirigiria-che à página de gestom da loja."]])))))

(defn expired-link []
  (html5-ok "Ligaçom caducada"
            [[:h1 "Ligaçom caducada"]
             [:p "Passou o dia, passou a romaria."]]))

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

  ;; send reset email for existing shopkeeper
  (let [[[eid email]]
        (seq
         (lcrux/q
          node
          '{:find [eid email]
            :where [[eid :loja.shopkeeper/email email]]}))]
    (send-reset-email (assoc (config/load "dev")
                             :crux-node node)
                      email
                      eid
                      false))

  (add-shopkeeper (assoc (config/load "dev")
                         :crux-node node)
                  "Manolo Peres"
                  "euccastro@gmail.com")

  ;; to test expiration
  (add-shopkeeper (assoc (config/load "dev")
                         :crux-node node)
                  "Manolo Peres"
                  "euccastro@gmail.com"
                  true)

  (.close node)
  )
