(ns cryptomilli.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [devtools.core :as devtools]
            [cljsjs.material-ui]
            [cryptomilli.events]
            [cryptomilli.subs]
            [cryptomilli.views :as views]
            [cryptomilli.config]))


;; Debugging helpers
(devtools/install!)
(enable-console-print!)

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (r/render [views/main-panel]
            (.getElementById js/document "app")))
