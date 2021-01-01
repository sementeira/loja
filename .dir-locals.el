;;; Directory Local Variables
;;; For more information see (info "(emacs) Directory Variables")

((nil
  (cider-default-cljs-repl . shadow)
  (cider-shadow-default-options . "app"))
 (clojure-mode
  (cider-clojure-cli-global-options . "-A:dev:test"))
 (clojurescript-mode
  (cider-clojure-cli-global-options . "-A:shadow-cljs:cljs-dev")))
