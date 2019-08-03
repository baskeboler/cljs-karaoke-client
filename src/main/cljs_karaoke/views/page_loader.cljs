(ns cljs-karaoke.views.page-loader
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as subscriptions]
            [stylefy.core :as stylefy]))

(def loader-logo
  {:display :block
   :position :absolute
   :height "60vh"
   :width "60vw"
   :top "50%"
   :left "50%"
   :transform "translate(-50%, -50%)"})
(defn page-loader-component []
  [:div.pageloader
   {:class (if @(rf/subscribe [::subscriptions/pageloader-active?])
             ["is-active"]
             [])}
   [:img (stylefy/use-style
          loader-logo
          {:src "/images/logo-2.svg"
           :alt ""})]])
