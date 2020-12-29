(ns loja.crux-test
  (:require [clojure.test :as t :refer [deftest is]]
            [crux.api :as crux]
            [loja.crux :as sut]
            [clj-uuid :as uuid]))

(def ^:dynamic *crux-node* nil)

(defn with-crux [f]
  (let [node (sut/crux-node nil)]
    (binding [*crux-node* node]
      (f))
    (sut/close node)))

(deftest crux-node
  (let [node (sut/crux-node "target/tmp/db")
        eid (or (sut/q1 node
                        '{:find [eid]
                          :where [[eid :test "data"]]})
                (uuid/v1))]
    (sut/sync-tx
     node
     [[:crux.tx/match eid nil]
      [:crux.tx/put
       {:crux.db/id eid
        :test "data"}]])
    (is (sut/q1 node
                '{:find [eid]
                  :where [[eid :test "data"]]})
        #{[eid]})))

(deftest tx-functions
  (let [fn-body
        '(fn [ctx eid attr]
           (let [db (crux.api/db ctx)
                 ent (crux.api/entity db eid)]
             [[:crux.tx/put (update ent attr inc)]]))
        _ (sut/add-transaction-function! :inc-attr fn-body)
        node (sut/crux-node nil)
        eid (uuid/v1)]
    (is (= {:inc-attr fn-body}
           @#'sut/transaction-functions))
    (is (= fn-body
           (sut/q1 node '{:find [body]
                          :where [[:inc-attr :crux.db/fn body]]})))
    (sut/sync-tx
     node
     [[:crux.tx/put
       {:crux.db/id eid :test 123}]])
    (is (sut/sync-tx
         node
         [[:crux.tx/fn :inc-attr eid :test]]))
    (is (= {:crux.db/id eid :test 124}
           (crux/entity (crux/db node) eid)))
    (sut/close node)))

(t/use-fixtures :each with-crux)

(deftest q
  (let [eid (uuid/v1)]
    (when (sut/sync-tx
           *crux-node*
           [[:crux.tx/put
             {:crux.db/id eid
              :test "data"}]])
      (is (= (sut/q *crux-node*
                    '{:find [eid]
                      :where [[eid :test "data"]]})
             #{[eid]})))))

(deftest q1
  (let [eid (uuid/v1)]
    (when (sut/sync-tx
           *crux-node*
           [[:crux.tx/put
             {:crux.db/id eid
              :test "data"}]])
      (is (= (sut/q1 *crux-node*
                    '{:find [eid]
                      :where [[eid :test "data"]]})
             eid)))))

(deftest sync-tx
  (let [eid (uuid/v1)]
    (is (sut/sync-tx
         *crux-node*
         [[:crux.tx/match eid nil]
          [:crux.tx/put
           {:crux.db/id eid
            :test "data"}]]))
    (is (= (sut/q *crux-node*
                  '{:find [eid]
                    :where [[eid :test "data"]]})
           #{[eid]}))
    ;; failing match
    (is (not (sut/sync-tx
              *crux-node*
              [[:crux.tx/match eid nil]
               [:crux.tx/put
                {:crux.db/id eid
                 :test "data"}]])))))

(deftest update-entity
  (let [eid (uuid/v1)]
    (sut/sync-tx
     *crux-node*
     [[:crux.tx/put
       {:crux.db/id eid
        :test "data"}]])
    (is (sut/update-entity *crux-node* eid assoc
                           :test "new-data"
                           :test2 "data2"))
    (is (= (crux/entity (crux/db *crux-node*) eid)
           {:crux.db/id eid
            :test "new-data"
            :test2 "data2"}))))
