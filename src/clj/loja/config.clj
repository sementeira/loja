(ns loja.config
  (:refer-clojure :exclude [load])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn load [which]
  (edn/read-string
   (slurp
    (io/resource (str "config/" which ".edn")))))

(comment
  (load "dev")
  )
