(ns loja.crypto-test
  (:require [loja.crypto :as sut]
            [clojure.test :as t :refer [deftest is]]))

(deftest round-trip
  (is (= (-> {:test "data"}
             (sut/urlsafe-encrypt "password")
             (sut/decrypt-urlsafe "password"))
         {:test "data"})))
