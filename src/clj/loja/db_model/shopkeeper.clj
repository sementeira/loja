(ns loja.db-model.shopkeeper
  (:require [buddy.hashers :as hashers]
            [clj-uuid :as uuid]
            [crux.api :as crux]
            [loja.schema :as schema]
            [loja.crux :as lcrux]))

(defn add-shopkeeper [crux-node display-name email]
  (when (seq
         (crux/q
          (crux/db crux-node)
          '{:find [eid]
            :in [email]
            :where [[eid :loja.shopkeeper/email email]]}
          email))
    (throw (ex-info "email exists" {:email email})))
  (let [eid (uuid/v1)
        e {:crux.db/id eid
           :loja.shopkeeper/display-name display-name
           :loja.shopkeeper/email email}
        err (schema/shopkeeper-explainer e)
        _ (when err (throw (ex-info "invalid data" err)))]
    (when (lcrux/sync-tx
           crux-node
           [[:crux.tx/match eid nil]
            [:crux.tx/put e]])
      eid)))

(defn set-password [crux-node eid password]
  (assert (crux/entity (crux/db crux-node) eid))
  (lcrux/update-entity
   crux-node
   eid
   assoc :loja.shopkeeper/password (hashers/derive password)))

(comment

  (def crux-node (crux/start-node {}))

  (add-shopkeeper crux-node "Manolo Gomes" "manolo@gomes.gal")

  (lcrux/close crux-node)
  )
