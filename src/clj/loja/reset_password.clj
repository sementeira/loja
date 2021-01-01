(ns loja.reset-password
  (:require
   [buddy.hashers :as hashers]
   [loja.crypto :as crypto]
   [loja.db-model.shopkeeper :as db-sk]
   [loja.email :as email]
   [loja.layout :refer [html5-ok]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.util.http-response :refer [see-other]]
   [taoensso.timbre :as log]))

(def one-hour-ms (* 1000 60 60))
(def one-day-ms (* one-hour-ms 24))

(def initial-set-expiration (* 3 one-day-ms))
(def recovery-expiration one-hour-ms)

(def ^:private msg
  "Olá,

Para estabelecer a tua senha clica no seguinte endereço (caduca em %s):

")

(defn display-ms [ms]
  (let [hours (quot ms one-hour-ms)
        days (quot hours 24)
        hours (mod hours 24)
        days? (not (zero? days))
        hours? (not (zero? hours))]
    (str
     (when days?
       (str days " dia" (when (> days 1) "s")))
     (when (and days? hours?)
       " e ")
     (when hours?
       (str hours " hora" (when (> hours 1) "s"))))))

(defn reset-password-body
  [callback-host eid password expiration-ms]
  (str
   (format msg (display-ms expiration-ms))
   callback-host
   "/cb/"
   (crypto/urlsafe-encrypt
    [:set-password
     {:id eid
      :expiration (+ (System/currentTimeMillis)
                     expiration-ms)}]
    password)))

(defn send-reset-email [{:keys [callback-host
                                password]
                         :as system}
                        email
                        subject
                        eid
                        expiration]
  (email/send-email
   system
   {:to email
    :subject subject
    :body (reset-password-body
           callback-host
           eid
           password
           expiration)}))

(defn add-shopkeeper
  "To call from the REPL"
  [{:keys [crux-node]
    :as system}
   display-name
   email]
  (log/info "add-shopkeeper" display-name email)
  (if-let [eid (db-sk/add-shopkeeper crux-node display-name email)]
    (send-reset-email
     system email
     "Invitaçom para atender a loja da Semente"
     eid
     initial-set-expiration)
    (throw (ex-info "could not transact shopkeeper" {}))))

(def errors
  {"nom-quadram" "As senhas nom quadram"})

(defn forgotten-password [error]
  (html5-ok
   "Recupera a tua senha"
   [[:h1 "Recupera a tua senha"]
    [:div "Di-me o teu email e mando-che umha ligaçom para restabelecer a tua senha."]
    (when error [:div {:style "color: red"} (errors error)])
    [:form {:method :post
            :action "/esquecim-senha"}
     [:input {:type "hidden"
              :name "csrf-token"
              :value *anti-forgery-token*}]
     [:div
      [:label {:for "email"} "Email"]
      [:input#email {:type :email :name "email"}]]
     [:div
      [:input {:type :submit :value "Enviar"}]]]]))

(defn send-recovery-email [{:keys [crux-node] :as system} email]
  (if-let [eid (db-sk/by-email crux-node email)]
    (do (log/info "Sending recovery email to" email)
        (send-reset-email
         system
         email
         "Restabelece a tua senha"
         eid
         recovery-expiration))
    (log/info "Not sending recovery email to unknown email" (pr-str email)))
  (see-other "/email-enviado"))

(defn email-sent []
  (html5-ok "Email Enviado"
            [[:h1 "Email Enviado"]
             [:div "Se esse endereço dá certo, há-che chegar um email com instruçons para restabelecer a tua senha."]]))

(defn handle-callback [password payload error]
  (let [[op {:keys [expiration]}]
        (crypto/decrypt-urlsafe payload password)]
    (assert (= op :set-password))
    (if (< expiration (System/currentTimeMillis))
      (see-other "/caducada")
      (html5-ok
       "Estabelece a tua senha"
       [[:h1 "Estabelece a tua senha"]
        (when error [:div {:style "color: red"} (errors error)])
        [:form {:method :post
                :action "/estabelece-senha"}

         [:input {:type "hidden"
                  :name "csrf-token"
                  :value *anti-forgery-token*}]
         [:input {:type :hidden
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
                      initial-set-expiration))

  (add-shopkeeper (assoc (config/load "dev")
                         :crux-node node)
                  "Manolo Peres"
                  "euccastro@gmail.com")

  (.close node)
  )
