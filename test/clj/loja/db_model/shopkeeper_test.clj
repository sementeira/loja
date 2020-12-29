(ns loja.db-model.shopkeeper-test
  (:require [loja.db-model.shopkeeper :as sut]
            [clojure.test :as t :refer [deftest is]]
            [crux.api :as crux]
            [loja.crux :as lcrux]))

(def ^:dynamic *crux-node* nil)

(defn with-crux [f]
  (let [node (crux/start-node {})]
    (binding [*crux-node* node]
      (f))
    (lcrux/close node)))

(t/use-fixtures :each with-crux)

(deftest prova
  (let [eid (sut/add-shopkeeper
             {:crux-node *crux-node*}
             "Manolo Gomes"
             "manolo@gomes.gal")]
    (is (uuid? eid))
    (is (= (crux/entity (crux/db *crux-node*) eid)
           {:crux.db/id eid
            :loja.shopkeeper/display-name "Manolo Gomes"
            :loja.shopkeeper/email "manolo@gomes.gal"}))
    (is (thrown? clojure.lang.ExceptionInfo (sut/add-shopkeeper
                                             {:crux-node *crux-node*}
                                             "Joaquim Gomes"
                                             "manolo@gomes.gal")))))

(deftest prova-invalid-data
  (is (thrown? clojure.lang.ExceptionInfo
               (sut/add-shopkeeper
                {:crux-node *crux-node*}
                "Joaquim Gomes"
                "manolo@gomes@gal"))))
