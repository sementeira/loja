(ns loja.config-test
  (:require [loja.config :as sut]
            [clojure.test :as t :refer [deftest is]]))

(deftest smoke-test
  (is (= {:a 1
          :b "string"
          :c true}
         (sut/load "test"))))
