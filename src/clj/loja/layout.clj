(ns loja.layout
  (:require
   [hiccup.core :refer [html]]
   [hiccup.page :refer [doctype]]
   [ring.util.http-response :refer [content-type ok]]))

(defn html5-ok
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

(html5-ok "" "");; => {:status 200, :headers {"Content-Type" "text/html; charset=utf-8"}, :body "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\" /><meta content=\"width=device-width, initial-scale=1.0\" name=\"viewport\" /><title></title></head><body></body></html>"}
