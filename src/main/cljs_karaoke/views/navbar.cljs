(ns cljs-karaoke.views.navbar
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.songs :as songs]
            [cljs-karaoke.styles :refer [shadow-style]]
            [cljs-karaoke.subs.user :as user-subs]
            [cljs-karaoke.router :as router]
            [stylefy.core :as stylefy]))
(defn navbar-item [path name]
  [:a.navbar-item
   {:href path
    :on-click #(rf/dispatch [::events/set-navbar-menu-active? false])}
   name])
(def logo-styles
  {:height "100%"})
(defn navbar-component []
  (let [is-active? (rf/subscribe [::s/navbar-menu-active?])]
    [:nav.navbar.is-fixed-top.is-transparent
     (stylefy/use-style shadow-style)
     [:div.navbar-brand
      [:a.navbar-item
       {:href (router/url-for :home)} ;"#/"}
       [:img.header-logo
        (stylefy/use-style
         logo-styles
         {:title "header logo" :src "/images/sing.svg"})]]
      ;; [:object.header-logo
        ;; {:data "images/header-logo.svg"
         ;; :title "header logo"}]
      [:a
       {:role     :button
        :class    (concat
                   ["navbar-burger" "burger"]
                   (if @is-active? ["is-active"] []))
        :on-click #(rf/dispatch [::events/set-navbar-menu-active? (not @is-active?)])}
       [:span] [:span] [:span]]]
     [:div
      {:class (concat
               ["navbar-menu"]
               (if @is-active? ["is-active"] []))}
      [:div.navbar-start
       [navbar-item (router/url-for :home) "control"]
       [navbar-item (router/url-for :playlist) "playlist"]
       [navbar-item
        ;; (songs/song-url (if @(rf/subscribe [::s/current-song])
                          ;; @(rf/subscribe [::s/current-song])
                          ;; "thriller"
        ;; (router/url-for :song :song-name
                        ;; (if @(rf/subscribe [::s/current-song])
                          ;; @(rf/subscribe [::s/current-song])
                          ;; "thriller"))
        (router/url-for :playback)
        "playback"]
       #_[:a.navbar-item

           {:on-click #(do
                        (rf/dispatch [::views-events/set-current-view :song @(rf/subscribe [::s/current-song])])
                        ;; (rf/dispatch [::views-events/view-action-transition :go-to-playback])
                        (rf/dispatch [::events/set-navbar-menu-active? false]))}
           "playback"]
       [navbar-item  (router/url-for :editor) "lyrics editor"]]
      [:div.navbar-end
       (when @(rf/subscribe [::user-subs/user-ready?])
         [:div.navbar-item
          (:name @(rf/subscribe [::user-subs/user]))])]]]))

