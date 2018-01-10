(ns cryptomilli.graphs.arc
  (:require [rid3.core :as rid3]
            [reagent.core :as r]
            [cryptomilli.utils :as u]
            [re-frame.core :as rf]))

;; Example from:
;; http://bl.ocks.org/mbostock/5100636

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; vars
(def tau
  (* 2 js/Math.PI))

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


(defn create-arc [ratom]
  (let [width  (get-width ratom)
        radius (/ width 2)]
    (-> js/d3
        .arc
        (.innerRadius (* 0.7 radius))
        (.outerRadius radius)
        (.startAngle 0))))


(defn arc-tween [new-angle ratom]
  (fn [d]
    (let [end-angle   (aget d "endAngle")
          interpolate (js/d3.interpolate end-angle new-angle)]
      (fn [t]
        (let [arc (create-arc ratom)]
          (set! (.-endAngle d) (interpolate t))
          (arc d))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; svg
(defn svg [node ratom]
  (let [width  (get-width ratom)
        height (get-height ratom)]
    (-> node
        (.attr "width" width)
        (.attr "height" height))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; main container
(defn main-container [node ratom]
  (let [width  (get-width ratom)
        height (get-height ratom)]
    (-> node
        (.attr "transform" (str "translate(" (/ width 2)
                                ","
                                (/ height 2) ")")))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; background
(defn background [node ratom]
  (let [arc (create-arc ratom)]
    (-> node
        (.datum #js {:endAngle tau})
        (.style "fill" "#ddd")
        (.attr "d" arc))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; foreground
(defn foreground-common [node ratom]
  (let [arc (create-arc ratom)
        ;; gauge here is sum (total of crypto assets)
        gauge (:gauge @ratom)]

    (-> node
        ;; 0.0 <--- gauge ---> 1
        (.datum #js {:endAngle (* gauge tau)})
        ;; red -> pink -> yellow -> orange -> green
        (.style "fill" (u/gauge-signal gauge))
        (.on "mouseover" (fn []
                           (this-as this
                             (-> (js/d3.select this)
                                 (.style "fill" "grey")))))
        (.on "mouseout" (fn []
                          (this-as this
                            (-> (js/d3.select this)
                                (.style "fill" (u/gauge-signal gauge))))))
        (.attr "d" arc))))


(defn foreground-did-mount [node ratom]
  (foreground-common node ratom))


(defn foreground-did-update [node ratom]
  (foreground-common node ratom))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; viz
(defn viz [ratom]
  [rid3/viz
   {:id             "arc-tween"
    :ratom          ratom
    :svg            {:did-mount svg}
    :main-container {:did-mount main-container}
    :pieces
                    [{:kind      :elem
                      :class     "background"
                      :tag       "path"
                      :did-mount background}
                     {:kind       :elem
                      :class      "foreground"
                      :tag        "path"
                      :did-mount  foreground-did-mount
                      :did-update foreground-did-update}]}])


(defn GraphArc []
  (let [local-store (rf/subscribe [:local-pub-keys])
        tickers (rf/subscribe [:ticker-prices])
        ratom (r/atom {:gauge 0})]
    (fn []
      (let [local-keys-map (vals @local-store)
            tickers-sub @tickers
            ;; assumes that :balance is in $
            sum-balance (reduce + (map #(u/balance->usd % tickers-sub) local-keys-map))

            ;; only mapping 0 > 1 Million | 0.0 > 1.0
            angle (/ sum-balance (* 1 1000 1000))

            _ (swap! ratom assoc :gauge angle)]

        [viz ratom]))))
