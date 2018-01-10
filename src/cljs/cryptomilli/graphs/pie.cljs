(ns cryptomilli.graphs.pie
  (:require [rid3.core :as rid3]
            [reagent.core :as r]
            [cryptomilli.subs :as subs]
            [re-frame.core :as rf]
            [cryptomilli.utils :as u]))

;; Example from:
;; https://bl.ocks.org/mbostock/3887235

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; util

(defn get-width [ratom]
  (let [page-width (get @ratom :page-width)]
    (max (min 500
              (- page-width 100))
         400)))

(defn get-height [ratom]
  (let [width (get-width ratom)]
    width))

(defn get-radius [ratom]
  (let [width (get-width ratom)]
    (/ width 2)))

(def pie
  (-> js/d3
      .pie
      (.value (fn [d]
                (aget d "value")))
      (.sort nil)))

(defn prepare-dataset [ratom]
  (-> (clj->js (get @ratom :dataset))
      pie))

(defn create-path [ratom]
  (let [radius (get-radius ratom)]
    (-> js/d3
        .arc
        (.outerRadius (- radius 10))
        (.innerRadius 0))))

(defn create-label [ratom]
  (let [radius (get-radius ratom)]
    (-> js/d3
        .arc
        (.outerRadius (- radius 40))
        (.innerRadius (- radius 40)))))

(def color
  (-> js/d3
      (.scaleOrdinal #js ["#98abc5" "#8a89a6" "#7b6888" "#6b486b" "#a05d56" "#d0743c" "#ff8c00"])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; elements

(defn svg [node ratom]
  (let [width (get-width ratom)
        height (get-height ratom)]
    (-> node
        (.attr "width" width)
        (.attr "height" height))))

(defn main-container [node ratom]
  (let [width  (get-width ratom)
        height (get-height ratom)]
    (-> node
        (.attr "transform" (str "translate("
                                (/ width 2)
                                ","
                                (/ height 2) ")")))))

(defn arc [node ratom]
  (let [path (create-path ratom)]
    (-> node
        (.attr "d" path)
        (.attr "fill" (fn [d]
                        (color (aget d "data" "pub-addr"))))
        (.on "mouseover" (fn []
                           (this-as this
                             (-> (js/d3.select this)
                                 (.style "fill" "grey")))))
        (.on "mouseout" (fn [d]
                          (this-as this
                            (-> (js/d3.select this)
                                (.style "fill" (color (aget d "data" "pub-addr")))))))
        (.style "stroke" "#FFF"))))

(defn text-label [node ratom]
  (let [label (create-label ratom)]
    (-> node
        (.attr "transform" (fn [d]
                             (str "translate(" (.centroid label d) ")")))
        (.attr "dy" "0.35em")
        (.text (fn [d]
                 (aget d "data" "pub-addr")))

        (.style "font" "10px sans-serif")
        (.style "text-anchor" "middle"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; viz

(defn viz [ratom]
  [rid3/viz
   {:id              "piechart"
    :ratom           ratom
    :svg             {:did-mount svg}
    :main-container  {:did-mount main-container}
    :pieces
                     [{:kind      :elem-with-data
                       :class     "arc"
                       :tag       "path"
                       :did-mount arc
                       :prepare-dataset prepare-dataset}
                      {:kind      :elem-with-data
                       :class     "text-label"
                       :tag       "text"
                       :did-mount text-label
                       :prepare-dataset prepare-dataset}]}])

(defn Alloc-by-tickers []
  (let [local-store (rf/subscribe [:local-pub-keys])
        tickers (rf/subscribe [:ticker-prices])
        ratom (r/atom {})]
    (fn []

      ;; get ready for some abuse of let
      (let [local-keys-map (vals @local-store)
            tickers-sub @tickers

            ;; keeping :pub-addr to avoid code duplication and complecting
            ;; initializing with 0.01 > for nice painting (before any actions)
            initial [{:pub-addr "BTC" :value 0.01}
                     {:pub-addr "ETH" :value 0.01}
                     {:pub-addr "LTC" :value 0.01}]

            fmt (fn [addr-map]
                  (let [{:keys [ticker]} addr-map]
                    {:pub-addr ticker
                     :value (u/balance->usd addr-map tickers-sub)}))

            dataset (mapv fmt local-keys-map)

            btcs (filter #(= (:pub-addr %) "BTC") dataset)
            btc-sum (reduce + (mapv :value btcs))
            eths (filter #(= (:pub-addr %) "ETH") dataset)
            eth-sum (reduce + (mapv :value eths))
            ltcs (filter #(= (:pub-addr %) "LTC") dataset)
            ltc-sum (reduce + (mapv :value ltcs))
            ordered-crypto [btc-sum eth-sum ltc-sum]

            ;; tested with nil :value or :pub-addr
            ;; all good > ignored in computations

            result (reduce-kv
                     (fn [acc index value]
                       (update-in acc [index :value] + value))
                     initial
                     ordered-crypto)

            _ (swap! ratom assoc :dataset result)]

           [viz ratom]))))
