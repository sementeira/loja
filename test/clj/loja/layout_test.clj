(ns loja.layout-test
  (:require [loja.layout :as sut]
            [clojure.test :as t :refer [deftest is]]))

(deftest empty
  (is (= {:status 200,
          :headers {"Content-Type" "text/html; charset=utf-8"},
          :body "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\" /><meta content=\"width=device-width, initial-scale=1.0\" name=\"viewport\" /><title></title></head><body></body></html>"}
         (sut/html5-ok "" []))) )

(deftest basic
  (is (= {:status 200,
          :headers {"Content-Type" "text/html; charset=utf-8"},
          :body "<!DOCTYPE html>\n<html><head><meta charset=\"UTF-8\" /><meta content=\"width=device-width, initial-scale=1.0\" name=\"viewport\" /><title>the title</title></head><body><div>the body</div><div>other div</div></body></html>"}
         (sut/html5-ok "the title" [[:div "the body"] [:div "other div"]]))) )
