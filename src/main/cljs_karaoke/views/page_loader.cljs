(ns cljs-karaoke.views.page-loader
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as subscriptions]
            [stylefy.core :as stylefy]
            [cljs-karaoke.embed :as embed :include-macros true]
            [cljs.core.async :as async :refer [chan <! >! go-loop go]]
            [bardo.interpolate :as interpolate]
            [bardo.ease :as ease]
            [bardo.transition :as transition]
            [cljs-karaoke.animation :refer [logo-animation]])

  (:require-macros [cljs-karaoke.embed :refer [inline-svg]]))

(defn page-loader-logo []
  [:figure.image.is-3by1.is-128x128
   [:img {:src "images/header-logo.svg"}]])

(defn page-loader-component []
      [:div.pageloader
       {:class (if @(rf/subscribe [::subscriptions/pageloader-active?])
                 ["is-active"]
                 [])}])
       ;; [:img ;(stylefy/use-style)
       ;; loader-logo
       ;; [logo-animation]])
       ;; [page-loader-logo]])
       ;; {:src "/images/logo-2.svg"
         ;; :alt ""}])})))
