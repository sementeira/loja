(ns loja.web
  (:require
   [clojure.pprint :refer [pprint]]
   [loja.auth :as auth]
   [loja.layout :refer [html5-ok]]
   [loja.reset-password :as rp]
   [reitit.ring :as rring]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.util.http-response :refer [not-found]]
   [taoensso.timbre :as log])
  (:import [org.eclipse.jetty.server Server]))


(defn echo [req]
  (html5-ok "Echo" [[:div "Prova"]
                    [:pre (with-out-str (pprint req))]]))

(defn routes [{:keys [crux-node password] :as config}]
  (rring/ring-handler
   (rring/router
    [["/echo" {:get #(echo %)}]
     ["/entrar"
      {:get (fn [{{:keys [redirigir-a]} :params
                  :as req}]
              (auth/login redirigir-a))
       :post (fn [req]
               (auth/handle-login crux-node req))}]
     ["/sair"
      {:get (fn [_]
              (auth/logout))
       :post @#'auth/handle-logout}]
     ["/abur"
      {:get (fn [_] (auth/bye))}]
     ["/boas-vindas" {:middleware [auth/wrap-restricted]
                      :get (fn [req]
                             (auth/boas-vindas
                              crux-node
                              (-> req :session :identity)))}]
     ["/restringido" {:middleware [auth/wrap-restricted]
                      :get (constantly (html5-ok "Entrastes!" [[:h1 "Mobiám!"]]))}]
     ["/esquecim-senha" {:get (fn [{{:keys [erro]} :params}]
                                (rp/forgotten-password erro))
                         :post (fn [{{:keys [email]} :params}]
                                 (rp/send-recovery-email config email))}]
     ["/email-enviado" {:get (fn [_] (rp/email-sent))}]
     ["/caducada" {:get (fn [_] (rp/expired-link))}]
     ["/cb/:payload" {:get (fn [{{:keys [payload]} :path-params
                                 {:keys [erro]} :params
                                 :as req}]
                             (rp/handle-callback
                              password
                              payload
                              erro))}]
     ["/estabelece-senha" {:post (fn [{{:keys [payload pass1 pass2]} :params
                                       :as req}]
                                   (rp/reset-password crux-node
                                                      password
                                                      payload
                                                      pass1
                                                      pass2))}]])
   (rring/create-default-handler
    {:not-found (constantly (not-found "Que?"))})))

(defn wrap-log [handler]
  (fn [req]
    (log/info "request" (pr-str req))
    (let [response (handler req)]
      (log/info "response" (pr-str req) (pr-str response))
      response)))

(defn override-login [handler eid]
  (if eid
    (fn [req]
      (handler (assoc-in req [:session :identity] eid)))
    handler))

(defn handler [{:keys [override-logged-in-as]
                :as config}]
  (-> (routes config)
      wrap-log
      auth/wrap-auth
      (wrap-anti-forgery
       {:read-token (fn [req]
                      (or (get-in req [:params :csrf-token])
                          (get-in req [:body-params :csrf-token])))})
      (override-login @override-logged-in-as)
      (wrap-session {:store (:session-store config)})
      wrap-keyword-params
      wrap-params))

(defn dev-handler
  "Creates the handler for every request, for REPL friendliness"
  [config]
  (fn [req]
    ((handler config) req)))

(defn start-server [http-port http-handler]
  (let [port (or http-port 62000)]
    (jetty/run-jetty http-handler {:port port :join? false})))

(defn stop-server [server]
  (.stop ^Server server))


(comment

  (require '[miracle.save :as ms])
  (def req {:ssl-client-cert nil,
            :protocol "HTTP/1.1",
            :remote-addr "127.0.0.1",
            :headers
            {"accept"
             "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
             "upgrade-insecure-requests" "1",
             "user-agent"
             "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:84.0) Gecko/20100101 Firefox/84.0",
             "connection" "keep-alive",
             "host" "localhost:62000",
             "accept-language" "gl-GL,gl;q=0.8,en-US;q=0.5,en;q=0.3",
             "accept-encoding" "gzip, deflate"},
            :server-port 62000,
            :content-length nil,
            :content-type nil,
            :character-encoding nil,
            :uri "/provaxxx",
            :server-name "localhost",
            :query-string nil,
            :body nil
            :scheme :http,
            :request-method :get})

  (def h (handler nil))

  (h req)

  )
