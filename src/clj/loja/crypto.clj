(ns loja.crypto
  (:require [buddy.core.codecs :as codecs]
            [buddy.core.crypto :as crypto]
            [buddy.core.hash :as hash]
            [buddy.core.nonce :as nonce]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def ^:private bytes->b64u-str (comp codecs/bytes->str codecs/bytes->b64u))
(def ^:private b64u-str->bytes (comp codecs/b64u->bytes codecs/to-bytes))

(defn urlsafe-encrypt
  "password should be a string at least 32 chars long"
  [data password]
  (let [iv (nonce/random-bytes 16)
        k (hash/sha256 password)
        msg (-> data pr-str codecs/to-bytes)
        enc (crypto/encrypt msg k iv)]
    (str (bytes->b64u-str iv) "~" (bytes->b64u-str enc))))

(defn decrypt-urlsafe
  [s password]
  (let [[iv-str enc-str] (str/split s #"~")
        enc (b64u-str->bytes enc-str)
        iv (b64u-str->bytes iv-str)
        k (hash/sha256 password)
        bs (crypto/decrypt enc k iv)]
    (edn/read-string
     (codecs/bytes->str bs))))


(comment
  (-> {:a "provando 123..."}
      (urlsafe-encrypt  "senha")
      (decrypt-urlsafe "senha"))
  )
