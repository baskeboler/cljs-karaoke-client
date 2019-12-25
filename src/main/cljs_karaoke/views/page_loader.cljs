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
   :z-index    1000
   :width      "100vw"
   :height     "100vh"
   :top        0
   :left       0
   :animation "slide-in-bck-center 0.9s ease-in both"})

(defn page-loader-logo []
  [:img
   (stylefy/use-style
    {:animation-name            "pulsate-bck"
     :animation-iteration-count :infinite
     :animation-duration        "1s"
     :animation-fill-mode       :both
     :animation-timing-function :ease-in-out}
    {:src "images/header-logo.svg"})])

(def page-loader-logo-2-styles
  {:font-family               "'Frijole', cursive"
   :font-size                 "5em"
   :animation-name            "pulsate-bck"
   :animation-iteration-count :infinite
   :animation-duration        "1s"
   :animation-timing-function :ease-in-out
   :animation-fill-mode       :both})
(defn page-loader-logo-2 []
  [:span
   (stylefy/use-style page-loader-logo-2-styles)
   @(rf/subscribe [::subscriptions/app-name])])
(defn page-loader-component []
  [:div.pageloader
   (stylefy/use-style (merge
                       pageloader-styles
                       (if-not @(rf/subscribe [::subscriptions/pageloader-active?])
                         {:animation "slide-out-blurred-bottom 1s ease both"
                          :animation-delay "1s"
                          :z-index 100})))
   [:div
    (stylefy/use-style (merge
                        styles/centered
                        {:z-index 1000}))
    [page-loader-logo-2]]])
 
