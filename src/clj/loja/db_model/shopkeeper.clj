(ns loja.db-model.shopkeeper
  (:require [clj-uuid :as uuid]
            [crux.api :as crux]
            [loja.schema :as schema]
            [loja.crypto :as crypto]
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
   assoc :loja.shopkeeper/hashed-password (crypto/hash-password password)))

(defn by-email [crux-node email]
  (lcrux/q1
   crux-node
   '{:find [eid]
     :in [email]
     :where [[eid :loja.shopkeeper/email email]]}
   email))

(defn get-hashed-password [crux-node eid]
  (lcrux/q1
   crux-node
   '{:find [pass]
     :in [eid]
     :where [[eid :loja.shopkeeper/hashed-password pass]]}
   eid))

(comment

  (def crux-node (crux/start-node {}))
  ;; or
  (do
    (require '[loja.system :as system])
    (def crux-node (:crux-node system/system)))

  (by-email crux-node "euccastro@gmail.com")
  (set-password
   crux-node
   (add-shopkeeper
    crux-node
    "Manolo Gomes"
    "euccastro@gmail.com")
   "abracadabra")

  (lcrux/close crux-node)
  )
