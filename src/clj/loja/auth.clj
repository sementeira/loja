(ns loja.auth
  (:require
   [better-cond.core :as b]
   [buddy.auth :refer [authenticated?]]
   [buddy.auth.accessrules :refer [restrict]]
   [buddy.auth.backends.session :refer [session-backend]]
   [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
   [crux.api :as crux]
   [loja.crypto :as crypto]
   [loja.db-model.shopkeeper :as db-sk]
   [loja.layout :refer [html5-ok]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.util.http-response :refer [content-type see-other]]))

(defn login [redirect-to]
  (html5-ok
   "Quem es?"
   [[:form {:method "post"
            :action "/entrar"}
     [:input {:type "hidden" :name "csrf-token" :value *anti-forgery-token*}]
     [:input {:type "hidden" :name "redirigir-a" :value redirect-to}]
     [:div
      [:label "Email"
       [:input {:type "email" :name "email" :required true}]]]
     [:div
      [:label "Senha"
       [:input {:type "password" :name "senha" :required true}]]]
     [:div
      [:input {:type "submit" :value "Entra"}]]]]))

(defn auth-error [{:keys [uri]} _]
  (->
   {:status 403
    :body (str "Access to " uri " is not authorized")}
   (content-type "text/plain; charset=utf-8")))

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error auth-error}))

(defn wrap-auth [handler]
  (let [backend (session-backend)]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn- credentials->shopkeeper [crux-node email password]
  (b/cond
    :when-let [sk-id (db-sk/by-email crux-node email)
               hashed-password (db-sk/get-hashed-password crux-node sk-id)]
    :when (crypto/check-password password hashed-password)
    sk-id))

(defn handle-login [crux-node
                    {{:keys [email senha redirigir-a]}
                     :params
                     :as req}]
  (if-let [user-id (credentials->shopkeeper crux-node email senha)]
    (-> (see-other (if (seq redirigir-a) redirigir-a "/boas-vindas"))
        (assoc :session (assoc (:session req) :identity user-id)))
    ;; XXX: set error string in login instead
    (auth-error {:uri redirigir-a} nil)))

(defn boas-vindas [crux-node eid]
  (let [name (-> crux-node
                 crux/db
                 (crux/entity eid)
                 :loja.shopkeeper/display-name)]
    (html5-ok "Boas vindas" [[:h1 (str "Olá " name "!")]])))

(defn logout []
  (html5-ok "Marchas?"
            [[:h1 "Marchas?"]
             [:form {:method :post
                     :action "/sair"}
              [:input {:type "hidden"
                       :name "csrf-token"
                       :value *anti-forgery-token*}]
              [:input {:type "submit"
                       :name "submit"
                       :value "Marcho"}]]]))

(defn handle-logout [req]
  (-> (see-other "/abur")
      (assoc :session (dissoc (:session req) :identity))))

(defn bye []
  (html5-ok "Abur" [[:h1 "Abur!"] [:p "Já estás fora."]]))

(defn routes [{:keys [crux-node] :as config}]
  [""
   ["/entrar"
    {:get (fn [{{:keys [redirigir-a]} :params
                :as req}]
            (login redirigir-a))
     :post (fn [req]
             (handle-login crux-node req))}]
   ["/sair"
    {:get (fn [_]
            (logout))
     :post @#'handle-logout}]
   ["/abur"
    {:get (fn [_] (bye))}]
   ["/boas-vindas" {:middleware [wrap-restricted]
                    :get (fn [req]
                           (boas-vindas
                            crux-node
                            (-> req :session :identity)))}]
   ["/restringido" {:middleware [wrap-restricted]
                    :get (constantly (html5-ok "Entrastes!" [[:h1 "Mobiám!"]]))}]
   ])
