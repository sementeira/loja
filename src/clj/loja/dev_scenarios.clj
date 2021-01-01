(ns loja.dev-scenarios
  (:require [crux.api :as crux]
            [loja.db-model.shopkeeper :as db-sk]))

(defn add-shopkeeper [{:keys [crux-node] :as config}
                      name
                      email
                      password]
  (let [eid (db-sk/add-shopkeeper
             crux-node
             name
             email)]
    (db-sk/set-password
     crux-node
     eid
     password)
    (assoc
     config
     :shopkeeper
     (crux/entity
      (crux/db crux-node)
      eid))))

(defn logged-in-as [{:keys [crux-node] :as config} email]
  (let [eid (db-sk/by-email crux-node email)]
    (assert eid)
    (reset! (-> config
                :handler-config
                :override-logged-in-as)
            eid)
    config))
