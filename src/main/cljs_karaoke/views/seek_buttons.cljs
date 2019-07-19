(ns cljs-karaoke.views.seek-buttons
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.events.views :as view-events]))

(declare right-seek-hotspot)



(defn right-seek-hotspot []
  [:a.right-seek-hotspot
    {:on-click #(rf/dispatch [::view-events/show-seek-buttons])}
   " "])
(defn right-seek-component [seek-fn]
  (let [visible (rf/subscribe [::s/seek-buttons-visible?])]
    (if @visible
      [:a.right-seek-btn
       {:on-click seek-fn}
       [:i.fas.fa-forward]]
      [right-seek-hotspot])))

(defn left-seek-hotspot []
  [:a.left-seek-hotspot
   {:on-click #(rf/dispatch [::view-events/show-seek-buttons])}
   " "])

(defn left-seek-component [seek-fn]
  (let [visible (rf/subscribe [::s/seek-buttons-visible?])]
    (if @visible
      [:a.left-seek-btn
       {:on-click seek-fn}
       [:i.fas.fa-backward]]
      [left-seek-hotspot])))

(defn seek-component [fw-fn bw-fn]
  [:div
   [left-seek-component bw-fn]
   [right-seek-component fw-fn]])
