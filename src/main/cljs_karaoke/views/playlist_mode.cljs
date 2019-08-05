(ns cljs-karaoke.views.playlist-mode
  (:require [re-frame.core :as rf]
            [cljs-karaoke.playlists :as playlists]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.views.navbar :refer [navbar-component]]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.utils :as utils :refer [icon-button]]))
(defn playlist-controls [i song]
  [:div.playlist-controls.field.has-addons])


(defn load-song-btn []
  [:button.button
   [:span.icon>i.fa.fa-stream]])

(defn move-song-up-btn [pos]
  [:button.button
   {:on-click #(rf/dispatch [::playlist-events/move-song-up pos])}
   [:span.icon>i.fa.fa-arrow-up]])


(defn move-song-down-btn [pos]
  [:button.button
   {:on-click #(rf/dispatch [::playlist-events/move-song-down pos])}
   [:span.icon>i.fa.fa-arrow-down]])

(defn playlist-component []
  (let [pl (rf/subscribe  [::subs/playlist])
        current (rf/subscribe [::subs/current-song])]
    (if @pl
      [:table.table.is-fullwidth.is-hoverable
       [:thead>tr
        [:th "#"] [:th "Song"] [:th "Action"]]
       [:tbody
        (doall
         (for [[i s] (mapv vector (map inc (range)) (playlists/songs @pl))]
           ^{:key (str "playlist-row-" i)}
           [:tr
            {:class (if (= @current s) "is-selected" "")}
            [:td i]
            [:td s]
            [:td
             [:div.playlist-controls.field.has-addons
              [:div.control
               [load-song-btn]]
              [:div.control
               [move-song-up-btn (dec i)]]
              [:div.control
               [move-song-down-btn (dec i)]]]]]))]]
              
      [:div.playlist-unavailable.is-fullwidth
       [:h3 "Playlist is unavailable"]])))

(defn ^export playlist-view-component []
  [:div.playlist-view.container>div.columns
   [:div.column
    [:p.title "Playlist Mode"]
    [playlist-component]]])
