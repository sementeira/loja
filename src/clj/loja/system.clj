(ns loja.system
  (:require
   [loja.crux :as crux]))

(defn start [{:keys [crux-dir]}]
  (let [node (crux/crux-node crux-dir)]
    {:crux-node node}))

(defn stop [{:keys [crux-node]}]
  (crux/close crux-node))

(comment
  (require '[loja.config :as config])
  (def system (start (config/load "dev")))
  (stop system)
  )
