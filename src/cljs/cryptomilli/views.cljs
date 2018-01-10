(ns cryptomilli.views
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [cljsjs.material-ui]
            [reagent.core :as r]
            [cryptomilli.subs :as subs]
            [cryptomilli.utils :as utils]
            [cljs-react-material-ui.core :refer [get-mui-theme color]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as ic]
            [rid3.core :as rid3]
            [cryptomilli.graphs.pie :as gpie]
            [cryptomilli.graphs.bar :as gbar]
            [cryptomilli.graphs.arc :as garc]
            [reagent.core :as r]
            [cryptomilli.utils :as u]))


(def paper-base {:padding 20
                 :margin 40
                 :text-align "center"})


(defn pub-key-input [{:keys [pub-addr on-save on-stop]}]
  (let [val (r/atom pub-addr)
        stop #(do (reset! val "")
                  (when on-stop (on-stop)))
        save #(let [v (-> @val str str/trim)]
                (when (seq v)
                  (on-save v)
                  (stop)))]
    (fn [props]
      [ui/text-field (merge props
                            {:type :type
                             :value @val
                             :auto-focus true
                             :on-blur save
                             :on-change #(reset! val (-> % .-target .-value))
                             :on-key-down #(case (.-which %)
                                             13 (save)
                                             27 (stop)
                                             nil)})])))


(defn pub-key-entry
  []
  [pub-key-input
   {:id "new-pub-addr"
    :floating-label-text "Paste a new public address, then hit Enter..."
    :full-width true
    :on-save #(do
                ;; we need to save the pub-key with nil values > then update later
                (rf/dispatch [:add-pub-key %])
                (rf/dispatch [:request-address-data %]))}])


(defn pub-addr-list
  []
  (let [local-keys-map (vals @(rf/subscribe [:local-pub-keys]))
        tickers-sub @(rf/subscribe [:ticker-prices])
        sum-balance (reduce + (map #(u/balance->usd % tickers-sub) local-keys-map))]

    [ui/table {:selectable false}
     [ui/table-header {:adjust-for-checkbox false :display-select-all false}
      [ui/table-row
       [ui/table-header-column "No."]
       [ui/table-header-column "Address"]
       [ui/table-header-column "Ticker"]
       [ui/table-header-column "Amount"]]]
     [ui/table-body {:display-row-checkbox false}
      (doall
        (for [addr local-keys-map]
          [ui/table-row {:key (:id addr) :selectable false}
           [ui/table-row-column (:id addr)]
           [ui/table-row-column (:pub-addr addr)]
           [ui/table-row-column (:ticker addr)]
           [ui/table-row-column (u/curr-fmt (u/balance->usd addr tickers-sub))]]))]
     [ui/table-footer
      [ui/table-row
       [ui/table-row-column {:style {:text-align "right"}}
        [:h1 (str "Total: $" (u/curr-fmt sum-balance))]]]]]))


(defn home-page []
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme
                 ;; (aget js/MaterialUIStyles "DarkRawTheme")
                 {:palette {:text-color (color :green600)}})}

   [:div
    ;; Card 1: welcome -------------------------
    [ui/paper {:style paper-base}
     [:h1 "CryptoMilli"]
     [:p "Visualize your btc, eth, ltc crypto hodlings."]
     [pub-key-entry]]

    ;; Card 2: allocations by tickers ----------
    [ui/paper {:style paper-base}
     [:h2 "Hodling Allocations"]
     [gpie/Alloc-by-tickers]]

    ;; Card 3: a milli yet ---------------------
    [ui/paper {:style paper-base}
     [:h2 "Crypto Millionaire yet?"]
     [garc/GraphArc]]

    ;; Card 4: accounts ------------------------
    [ui/paper {:style (dissoc paper-base :text-align)}
     [pub-addr-list]]

    ;; Card 5: barchart ------------------------
    [ui/paper {:style paper-base}
     [:h2 "Allocations by addresses"]
     [gbar/GraphBar]]

    ;; Card 5: sponsor -------------------------
    [ui/paper {:style (dissoc paper-base :text-align)}
     [:h3 "Sponsor these upcoming features:"]
     [:p "~ Playing Lil Wayne's song when you're a milli. $" (u/curr-fmt 2000)]
     [:p "~ Addition of a requested crypto-currency. $" (u/curr-fmt 5000)]
     [:p "~ Visualize by storage i.e: hardware, software aka hot, cold. $" (u/curr-fmt 30000)]
     [:p "~ Inclusion of ERC20 Tokens. $" (u/curr-fmt 60000)]
     [:p "~ A web service (api) to get the balance of any major crypto-currency's address. All in one endpoint. $" (u/curr-fmt 150000)]
     [:p "~ Hodlings overtime. Meta-crunching of investing vs spending on all provided addresses. $" (u/curr-fmt 250000)]]
     ;; [:br] ;; [:p "Historical Hodlings: **dummy example**"]

    ;; Card 7: contact --------------------------
    [ui/paper {:style paper-base}
     [:p "Any ideas, sponsorships, suggestions for CryptoMilli, ping me on Twitter: "
      [:bold (u/new-target-link "@mohamedhayibor" "https://twitter.com/mohamedhayibor")]]]]])

(defn main-panel []
  [home-page])
