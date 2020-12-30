(ns loja.db-model.shopkeeper-test
  (:require [loja.db-model.shopkeeper :as sut]
            [clojure.test :as t :refer [deftest is]]
            [crux.api :as crux]
            [loja.crux :as lcrux]
            [loja.crypto :as crypto]))

(def ^:dynamic *crux-node* nil)

(defn with-crux [f]
  (let [node (crux/start-node {})]
    (binding [*crux-node* node]
      (f))
    (lcrux/close node)))

(t/use-fixtures :each with-crux)

(deftest add-shopkeeper
  (let [eid (sut/add-shopkeeper
             *crux-node*
             "Manolo Gomes"
             "manolo@gomes.gal")]
    (is (uuid? eid))
    (is (= (crux/entity (crux/db *crux-node*) eid)
           {:crux.db/id eid
            :loja.shopkeeper/display-name "Manolo Gomes"
            :loja.shopkeeper/email "manolo@gomes.gal"}))
    (is (thrown? clojure.lang.ExceptionInfo (sut/add-shopkeeper
                                             *crux-node*
                                             "Joaquim Gomes"
                                             "manolo@gomes.gal")))))

(deftest invalid-data
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/add-shopkeeper
                *crux-node*
                "Joaquim Gomes"
                "manolo@gomes@gal"))))

(deftest set-password
  (let [eid (sut/add-shopkeeper
             *crux-node*
             "Manolo Gomes"
             "manolo@gomes.gal")]
    (is (sut/set-password *crux-node* eid "abracadabra"))
    (is (crypto/check-password
         "abracadabra"
         (lcrux/q1
          *crux-node*
          {:find ['pass]
           :where [[eid :loja.shopkeeper/password 'pass]]})))))
