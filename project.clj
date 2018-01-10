(defproject cryptomilli "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.908"]
                 ;; clj

                 ;; cljs
                 [reagent "0.7.0"]
                 [rid3 "0.2.0"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [cljs-ajax "0.7.2"]
                 [cljs-react-material-ui "0.2.50"]
                 [cljsjs/react "15.6.1-1"]
                 [cljsjs/react-dom "15.6.1-1"]
                 [figwheel-sidecar "0.5.0"]
                 [binaryage/devtools "0.9.7"]
                 [re-frame "0.10.2"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :min-lein-version "2.5.3"

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]
                   [day8.re-frame/trace "0.1.13"]]

    :plugins      [[lein-figwheel "0.5.13"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "cryptomilli.core/mount-root"}
     :compiler     {:main                 cryptomilli.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true}
                    :source-map-timestamp true
                    :preloads             [devtools.preload day8.re-frame.trace.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            cryptomilli.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :simple
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
