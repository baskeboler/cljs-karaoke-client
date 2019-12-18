(ns cljs-karaoke.views.page-loader
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as subscriptions]
            [stylefy.core :as stylefy]
            [cljs-karaoke.embed :as embed :include-macros true]
            [cljs.core.async :as async :refer [chan <! >! go-loop go]]
            [bardo.interpolate :as interpolate]
            [bardo.ease :as ease]
            [cljs-karaoke.styles :as styles]
            [stylefy.core :as stylefy]
            [bardo.transition :as transition]
            [cljs-karaoke.animation :refer [logo-animation]])

  (:require-macros [cljs-karaoke.embed :refer [inline-svg]]))

(def pageloader-styles
  {:position   :fixed
   :display    :block
   :background :lightpink
   :z-index    10000
   :width      "100vw"
   :height     "100vh"
   :top        0
   :left       0
   :animation "slide-in-top 0.5s ease both"})

(defn page-loader-logo []
  [:img {:src "images/header-logo.svg"}])

(defn page-loader-component []
  [:div.pageloader
   (stylefy/use-style (merge
                       pageloader-styles
                       (if-not @(rf/subscribe [::subscriptions/pageloader-active?])
                         {:animation "slide-out-top 0.5s ease both"
                          :animation-delay "1s"})))
   [:div
    (stylefy/use-style (merge
                        styles/centered
                        {:z-index 1000}))
    [page-loader-logo]]])
