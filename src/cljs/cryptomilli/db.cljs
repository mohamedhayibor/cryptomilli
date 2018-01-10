(ns cryptomilli.db
  (:require [re-frame.core :as rf]
            [cljs.reader]))

;; -- Default db Value ------------------
(def default-db
  {:local-pub-keys (sorted-map)
   :ticker-prices {:btc nil
                   :eth nil
                   :ltc nil}})

;; -- Local Storage ---------------------
;; They (pub-keys) stick to the browser under default settings
;; When the user comes back the pub-keys are retrieved

(def ls-key "public-keys")       ;; localStore key (yes learning from re-frame docs :)

(defn pub-keys->local-store
  "Saves the public keys onto localStorage"
  [pub-keys]
  (.setItem js/localStorage ls-key (str pub-keys)))


;; -- cofx Registrations ----------------
;; `reg-cofx` will register a "coeffect handler" and
;; inject the pub-keys into localStore. (reads first then injects)

(rf/reg-cofx
  :local-store-pub-keys
  (fn [cofx _]
    (println ":db :local-store-pub-keys cofx: " cofx)
    ;; put the localStore pub-keys into the coeffect under
    ;; :local-store-pub-keys
    (assoc cofx :local-store-pub-keys
                ;; read in pub-keys from localStore, and process into a sorted map
                (into (sorted-map)
                      (some->> (.getItem js/localStorage ls-key)
                               (cljs.reader/read-string)))))) ;; EDN map -> map
