(ns cljs-karaoke.views.toasty
  (:require [re-frame.core :as rf]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.events :as events]
            [stylefy.core :as stylefy]))

(defn ^:export toasty []
  (when-let [toasty (rf/subscribe [::s/toasty?])]
    (when-let [_ (rf/subscribe [::s/initialized?])]
      [:div (stylefy/use-style
             (merge
              {:position   :fixed
               :bottom     "-474px"
               :left       "0"
               :opacity    0
               :z-index    2
               :display    :block
               :transition "all 0.5s ease-in-out"}
              (if @toasty {:bottom  "0px"
                           :opacity 1})))
       [:audio {:id    "toasty-audio"
                :src   "/media/toasty.mp3"
                :style {:display :none}}]
       [:img {:src "/images/toasty.png" :alt "toasty"}]])))

(defn ^:export trigger-toasty []
  (let [a (.getElementById js/document "toasty-audio")]
    (.play a)
    (rf/dispatch [::events/trigger-toasty])))
