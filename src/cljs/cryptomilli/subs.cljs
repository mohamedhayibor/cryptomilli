(ns cryptomilli.subs
  (:require [re-frame.core :as rf]
            [cryptomilli.db :as db]))

;; -- Only layer 2 accessors
;; -- Usage (subscribe [:local-pub-keys])
(rf/reg-sub
 :local-pub-keys
 (fn [db event-vec]
   (println ">>> Inside subs :local-pub-keys ------>")
   (println ">>> db: " db)
   (:local-pub-keys db)))

(rf/reg-sub
  :ticker-prices
  (fn [db _]
    (println ">>> Inside subs :ticker-prices ------>")
    (println ">>> db: " db)
    (:ticker-prices db)))
