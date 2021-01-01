(ns loja.shopkeeping
  (:require
   [loja.auth :as auth]
   [loja.db-model.color :as db-color]
   [loja.layout :refer [html5-ok]]
   [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
   [ring.util.http-response :refer [see-other]]
   [loja.crux :as lcrux]))

(defn colors [crux-node]
  (html5-ok
   "Cores"
   [[:h1 "Cores"]

    [:form {:method "post"
            :action "/cor"}
     [:input {:type "hidden" :name "csrf-token" :value *anti-forgery-token*}]
     (let [colors (sort-by
                   :loja.color/name
                   (db-color/get-colors crux-node))]
       (for [{:keys [crux.db/id loja.color/name loja.color/color]} colors]
         [:div
          [:input {:name (str "cor-" id)
                   :type "color"
                   :value (str "#" color)}]
          name
          [:a {:href (str "/apaga-cor?cor=" id)}
           "Apagar"]]))
     [:div
      [:label "Nome"
       [:input {:type "text" :name "nome" :required true}]]]
     [:div
      [:label "Cor"
       [:input {:type "color" :name "cor" :required true}]]]
     [:div
      [:input {:type "submit" :value "Acrescenta"}]]]]))

(defn add-color [crux-node name color]
  (let [result (db-color/add-color crux-node name color)]
    (assert result))
  (see-other "/cor"))

(defn routes [{:keys [crux-node] :as config}]
  ["" {:middleware [auth/wrap-restricted]}
   ["/cor" {:get (fn [_] (colors crux-node))
            :post (fn [{{:keys [nome cor]} :params
                        :as req}]
                    (add-color crux-node nome cor))}]])

(comment

  (require '[miracle.save :as ms])
  (ms/ld :colors)

  (require '[loja.system :as system])

  (def crux
    (:crux-node system/system))

  (require '[loja.crux :as lcrux])

  (lcrux/q crux '{:find [(eql/project eid [*])]
                  :where [[eid :loja.color/name]]})

  )
