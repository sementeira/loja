(ns loja.web
  (:require
   [clojure.pprint :refer [pprint]]
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]
   [ring.adapter.jetty :as jetty]
   [ring.util.http-response :refer [content-type ok]])
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
  (html5-ok "Echo" [[:div "Prova"]
                    [:pre (with-out-str (pprint req))]]))

(defn handler [crux-node]
  (fn [req]
    (handle crux-node req)))

(defn start-server [http-port http-handler]
  (let [port (or http-port 62000)]
    (jetty/run-jetty http-handler {:port port :join? false})))

(defn stop-server [server]
  (.stop ^Server server))

