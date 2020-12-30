(ns loja.dev-scenarios
  (:require [crux.api :as crux]
            [loja.db-model.shopkeeper :as db-sk]))

(defn add-shopkeeper [{:keys [crux-node] :as system}
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
     system
     :shopkeeper
     (crux/entity
      (crux/db crux-node)
      eid))))
