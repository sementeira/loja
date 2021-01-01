# Loja da Semente

Depois de clonar este repositório, usa isto para que github ignore as tuas
mudanças locais nos arquivos de configuraçom:

    git update-index --skip-worktree resources/config/*

Para desenvolver a parte do navegador com CIDER, isto é que me funcionou:

- cider-jack-in-clj (escolhe clojure-cli)

- cider-jack-in-cljs (responde que si, escolhe shadow-cljs, responde si de novo)

- arranca o servidor web e visita localhost:62000

- corre algumha cousa (e.g. `(js/console.log "ola")`) no buffer de cider de
  shadow:cljs.
  
- avalia algum namespace cljs e comprova que funciona.
