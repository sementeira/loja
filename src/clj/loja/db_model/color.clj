(ns loja.db-model.color
  (:require [clj-uuid :as uuid]
            [crux.api :as crux]
            [loja.schema :as schema]
            [loja.crux :as lcrux]))

(defn add-color [crux-node name color]
  (when (seq
         (crux/q
          (crux/db crux-node)
          '{:find [eid]
            :in [name]
            :where [[eid :loja.color/name name]]}
          name))
    (throw (ex-info "color exists" {:name name})))
  (let [eid (uuid/v1)
        e {:crux.db/id eid
           :loja.color/name name
           :loja.color/color (subs color 1)}
        err (schema/color-explainer e)
        _ (when err (throw (ex-info "invalid data" err)))]
    (when (lcrux/sync-tx
           crux-node
           [[:crux.tx/match eid nil]
            [:crux.tx/put e]])
      eid)))

(defn get-colors [crux-node]
  (map first
       (lcrux/q crux-node
                '{:find [(eql/project eid [*])]
                  :where [[eid :loja.color/name]]})))
