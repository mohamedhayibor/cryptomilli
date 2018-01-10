(ns cryptomilli.events
  (:require [re-frame.core :as rf]
            [cryptomilli.db :as db]
            [ajax.core :as ajax]
            [cryptomilli.utils :as u]
            [day8.re-frame.http-fx]))

;; -- Storing pub-keys to localStore --------
;; -- Runs after the event handler :add-pub-key
(def ->local-store (rf/after db/pub-keys->local-store))

;; -- Interceptors --------------------------
;; A chain of interceptors can be represented as a vector
;; These are triggered when user submits
(def pub-keys-interceptors [(rf/path :local-pub-keys)      ;; 1st param to handler will be the value from this path within db
                            ->local-store
                            (when ^boolean js/goog.DEBUG rf/debug) ;; js browser console for debug logs
                            rf/trim-v])                            ;; removes first (event id) element from the event vec


;; -- Taken from re-frame's todomvc example :)
(defn allocate-next-id
  "Returns the pub-key id.
  Assumes pub-keys are sorted.
  Returns one more than the current largest id."
  [pub-keys]
  ((fnil inc 0) (last (keys pub-keys))))


;; -- Event Handlers -------------------------
;;
;; There are 3 main -db events:
;; initialize-db | add-pub-key | delete-pub-key


;; Fetch related data from pre-existing pub-keys
;; We need an -fx coz we're pulling from LS
(rf/reg-event-fx
 :initialize-db

 ;; Retrieve former public addr(s) from localStorage if any
 [(rf/inject-cofx :local-store-pub-keys)]

 ;; the event handler being registered
 (fn [{:keys [db local-store-pub-keys] :as coefx} _]
   (println ">> inside :initialize-db>>>>")
   (println ">>>> coefx:>> " coefx)
   (println "local-store-pub-keys: " local-store-pub-keys)
   (println "db: " db)
   {:db (assoc db/default-db :local-pub-keys local-store-pub-keys)
    :dispatch [:request-ticker-prices]}))


;; Overall Strategy: add pub key with :balance nil
;; Then update params when data lands
(rf/reg-event-db
  :add-pub-key

  pub-keys-interceptors

  (fn [pub-keys [pub-key]]
    (println "[add-pub-k]>>>> pub-keys: " pub-keys)
    (println "[add-pub-k]>>>> pub-key: " pub-key)
    (let [id (allocate-next-id pub-keys)
          addr-patch (u/fix-eth-addr pub-key)]
      (assoc pub-keys id {:id id
                          :pub-addr addr-patch
                          :balance nil
                          :ticker (u/get-ticker pub-key)}))))


(rf/reg-event-db
  :delete-pub-key

  ;; looks after deleting a pub-key from local store
  pub-keys-interceptors

  (fn [pub-keys [id]]
    (dissoc pub-keys id)))

;; -- Talking to other services (only get requests)
;;
;; Due to availability issues and other uncertainties
;; We save the latest get request data to LS (local storage)
;; * (maybe) trigger updates every 5 minutes
;; * for responsiveness (we serve stale data then update when data lands)


;; -- Debugging notes
;; 1st arg: {:event [:request-address-data %], :db {:local-pub-keys {1 {:id 1, :pub-addr %}}}}
;; 2nd arg: [:request-address-data %] | (:event 1st)
(rf/reg-event-fx
  :request-address-data

  (fn [_ [_ addr]]
    ;; returning a map of side effects
    (println "Kicked request")
    (println "addr: " addr)

    {:http-xhrio {:method         :get
                  ;; https://www.blockcypher.com/dev/bitcoin/#address-balance-endpoint
                  :uri (u/route-get addr)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:address-data-loaded]
                  :on-failure [:failed-get-addr]}}))

(rf/reg-event-db
  :address-data-loaded

  ;; interceptor takes care of saving data to LS
  pub-keys-interceptors

  (fn [db [{:keys [address balance] :as payload}]]
    (println "Address data loaded......>>>>>XXX...")

    ;; 1. Find the id of the address
    ;; 2. Dispatch an event to update address data:
    ;;    * from nil to actual amount
    (let [id-seq (u/id-by-addr db address)
          id (first id-seq)]
      (println "eth address: >>>>>> " address)
      (println "eth balance: >>>>>> " balance)
      (println "Payload> track eth addr: " payload)
      (println "(first id-seq)" id)
      (println "db before assoc-in [id :balance]: >>>>" db)
      (assoc-in db [id :balance] balance))))

(rf/reg-event-db
  :failed-get-addr

  (fn [db event]
    (println "Event: " event)
    (println ">>> Failed GET for address data")))

;; -- Fetching ticker-prices ----------------
(rf/reg-event-fx
  :request-ticker-prices

  (fn [_ _]
    (println ":request-ticker-prices fired: >>")

    {:http-xhrio {:method         :get
                  ;; assumes that btc, eth, ltc will be in the first 10
                  :uri "https://api.coinmarketcap.com/v1/ticker/?limit=10"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:ticker-prices-loaded]
                  :on-failure [:failed-get-tickers]}}))

(rf/reg-event-db
  :ticker-prices-loaded

  [(rf/path :ticker-prices)
   (rf/inject-cofx :local-store-pub-keys)
   ->local-store
   (when ^boolean js/goog.DEBUG rf/debug)
   rf/trim-v]

  ;; we only care about btc ltc eth prices for now
  ;; + we do not care about former prices (last refresh)
  (fn [db [tickers-vec]]
    (println ">>>> inside :ticker-prices-loaded")
    (let [symbols (mapv :symbol tickers-vec)
          btc-id (.indexOf symbols "BTC")
          btc-price (u/id->price tickers-vec btc-id)
          eth-id (.indexOf symbols "ETH")
          eth-price (u/id->price tickers-vec eth-id)
          ltc-id (.indexOf symbols "LTC")
          ltc-price (u/id->price tickers-vec ltc-id)]
      (println "Tickers-vec type: " tickers-vec)
      (println "eth-price: " eth-price)

      ;; saves to db aka in-memory
      (merge db {:btc btc-price
                 :eth eth-price
                 :ltc ltc-price}))))


(rf/reg-event-db
  :failed-get-tickers
  (fn [db event]
    (println ">> :failed-get-tickers")
    (println "db: " db)
    (println "event: " event)))
