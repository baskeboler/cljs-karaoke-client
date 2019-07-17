(ns cljs-karaoke.views.page-loader
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as subscriptions]))

(defn page-loader-component []
  [:div.pageloader
   {:class (if @(rf/subscribe [::subscriptions/pageloader-active?])
             ["is-active"]
             [])}
   [:span.title
    "Loading Karaoke"]])
