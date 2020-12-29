(ns loja.web
  (:require
   [clojure.pprint :refer [pprint]]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]
   [reitit.ring :as rring]
   [ring.adapter.jetty :as jetty]
   [ring.util.http-response :refer [content-type ok not-found]])
  (:import [org.eclipse.jetty.server Server]))

(defn- html5-ok
  ([title body]
   (html5-ok title [] body))
  ([title head body]
   (-> (html
        (doctype :html5)
        `[:html
          [:head
           [:meta {:charset "UTF-8"}]
           [:meta {:name "viewport"
                   :content "width=device-width, initial-scale=1.0"}]
           [:title ~title]
           ~@head]
          [:body
           ~@body]])
       ok
       (content-type "text/html; charset=utf-8"))))

(defn handle [crux-node req]
  (html5-ok "Echo" [[:div "Prova dous"]
                    [:pre (with-out-str (pprint req))]]))

(defn handle-callback [crux-node password req]
  (html5-ok "Callback" [[:div "Now I would handle callback"]
                        [:pre (with-out-str (pprint req))]]) )

(defn handler [crux-node password]
  (rring/ring-handler
   (rring/router
    [["/prova" #(handle crux-node %)]
     ["/pr" #(html5-ok "Yes" [[:div "Got it"] [:pre (pr-str %)]])]
     ["/cb/:payload" #(handle-callback crux-node password %)]])
   (rring/create-default-handler
    {:not-found (constantly (not-found "Que?"))})))

(defn dev-handler
  "Creates the handler for every request, for REPL friendliness"
  [crux-node password]
  (fn [req]
    ((handler crux-node password) req)))

(defn start-server [http-port http-handler]
  (let [port (or http-port 62000)]
    (jetty/run-jetty http-handler {:port port :join? false})))

(defn stop-server [server]
  (.stop ^Server server))


(comment

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
