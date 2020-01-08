(ns cljs-karaoke.views.playlist-mode
  (:require [re-frame.core :as rf]
            [cljs-karaoke.playlists :as playlists]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.events.views :as view-events]
            [cljs-karaoke.views.navbar :refer [navbar-component]]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.utils :as utils :refer [icon-button]]
            [cljs-karaoke.notifications :as notifications]
            [cljs-karaoke.protocols :as protocols]
            [cljs-karaoke.styles :refer [default-page-styles]]
            [stylefy.core :as stylefy]))
(defn playlist-controls [i song]
  [:div.playlist-controls.field.has-addons])


(defn load-song-btn [pos]
  [:button.button
   {:on-click #(rf/dispatch [::playlist-events/jump-to-playlist-position pos])}
   [:span.icon>i.fas.fa-fw.fa-folder-open]])

(defn play-song-btn [pos]
  [:button.button
   {:on-click #(do
                 (rf/dispatch [::playlist-events/jump-to-playlist-position pos])
                 (rf/dispatch [::view-events/view-action-transition :go-to-playback]))}
                               
   [:span.icon>i.fas.fa-fw.fa-play]])

(defn move-song-up-btn [pos]
  [:button.button
   {:on-click #(rf/dispatch [::playlist-events/move-song-up pos])}
   [:span.icon>i.fas.fa-fw.fa-arrow-up]])


(defn move-song-down-btn [pos]
  [:button.button
   {:on-click #(rf/dispatch [::playlist-events/move-song-down pos])}
   [:span.icon>i.fas.fa-fw.fa-arrow-down]])


(defn forget-delay-btn [ song-name]
  [:button.button.is-danger
   {:on-click #(rf/dispatch [::song-events/forget-custom-song-delay song-name])}
   [:span.icon>i.fas.fa-fw.fa-trash]])

(defn playlist-component []
  (let [pl (rf/subscribe  [::subs/playlist])
        current (rf/subscribe [::subs/current-song])]
    ;; (when-not (number? (protocols/current @pl))
      ;; (rf/dispatch-sync [::playlist-events/set-current-playlist-song])
    (if @pl
      [:table.table.is-fullwidth.is-hoverable
       [:thead>tr
        [:th "#"] [:th "Song"] [:th "Action"]]
       [:tbody
        (doall
         (for [[i s] (mapv vector (map inc (range)) (protocols/songs @pl))]
           ^{:key (str "playlist-row-" i)}
           [:tr
            {:class (if (= @current s) "is-selected" "")}
            [:td i]
            [:td s]
            [:td
             [:div.playlist-controls.field.has-addons
              [:div.control
               [load-song-btn (dec i)]]
              [:div.control
               [move-song-up-btn (dec i)]]
              [:div.control
               [move-song-down-btn (dec i)]]
              [:div.control
               [play-song-btn (dec i)]]
              [:div.control
               [forget-delay-btn s]]]]]))]]
      [:div.playlist-unavailable.is-fullwidth
       [:h3 "Playlist is unavailable"]])))

(defn ^:export playlist-view-component []
  [:div.playlist-view.container-fluid.slide-in-bck-center>div.columns
   (stylefy/use-style default-page-styles)
   [:div.column
    [:p.title "Playlist Mode"]
    [playlist-component]]])
