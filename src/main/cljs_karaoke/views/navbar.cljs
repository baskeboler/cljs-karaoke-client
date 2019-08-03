(ns cljs-karaoke.views.navbar
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf]
            [cljs-karaoke.events.views :as views-events]))


(defn navbar-component []
  [:nav.navbar.is-fixed-top.is-transparent
   [:div.navbar-brand
    [:a.navbar-item
     {:href "#/"}
     "aviatto karaoke"]
    [:div.navbar-burger.burger
     [:span] [:span] [:span]]]
   [:div.navbar-menu
    [:div.navbar-start
     [:a.navbar-item
      {:href "#/"}
       ;; :on-click #(rf/dispatch [::views-events/ view-action-transition :go-to-home])}
      "control"]
     [:a.navbar-item
      {:href "#/playlist"}
      "playlist"]
     [:a.navbar-item
      {:on-click #(rf/dispatch [::views-events/view-action-transition :go-to-playback])}
      "playback"]]]])

;; (defn with-navbar [other]
  ;; (->> (concat [(first other) [navbar]]  (rest other))
       ;; into []]]]])
