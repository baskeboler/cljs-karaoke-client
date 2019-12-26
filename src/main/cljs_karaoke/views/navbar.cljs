(ns cljs-karaoke.views.navbar
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :as rf]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.subs.user :as user-subs]))
(defn navbar-component []
  (let [is-active? (rf/subscribe [::s/navbar-menu-active?])]
    [:nav.navbar.is-fixed-top.is-transparent
     [:div.navbar-brand
      [:a.navbar-item
       {:href "#/"}
       [:i.fas.fa-fw.fa-grimace.fa-2x]
       [:h5
        @(rf/subscribe [::s/app-name])]]
      ;; [:object.header-logo
        ;; {:data "images/header-logo.svg"
         ;; :title "header logo"}]
      [:a
       {:role :button
        :class (concat
                ["navbar-burger" "burger"]
                (if @is-active? ["is-active"] []))
        :on-click #(rf/dispatch [::events/set-navbar-menu-active? (not @is-active?)])}
       [:span] [:span] [:span]]]
     [:div
      {:class (concat
               ["navbar-menu"]
               (if @is-active? ["is-active"] []))}
      [:div.navbar-start
       [:a.navbar-item
        {:href "#/"
         :on-click #(rf/dispatch [::events/set-navbar-menu-active? false])}
        "control"]
       [:a.navbar-item
        {:href "#/playlist"
         :on-click #(rf/dispatch [::events/set-navbar-menu-active? false])}
        "playlist"]
       [:a.navbar-item
        {:on-click #(do
                      (rf/dispatch [::views-events/view-action-transition :go-to-playback])
                      (rf/dispatch [::events/set-navbar-menu-active? false]))}
        "playback"]]
      [:div.navbar-end
       (when @(rf/subscribe [::user-subs/user-ready?])
         [:div.navbar-item
          (:name @(rf/subscribe [::user-subs/user]))])]]]))

;; (defn with-navbar [other]
  ;; (->> (concat [(first other) [navbar]]  (rest other))
       ;; into []]]]])
