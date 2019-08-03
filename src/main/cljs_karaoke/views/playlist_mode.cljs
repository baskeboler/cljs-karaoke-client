(ns cljs-karaoke.views.playlist-mode
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.views.navbar :refer [navbar-component]]))
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
            [:td]]))]]
      [:div.playlist-unavailable.is-fullwidth
       [:h3 "Playlist is unavailable"]])))

(defn ^export playlist-view-component []
  
  [:div.playlist-view.container>div.columns
   [:div.column
    [:p.title "Playlist Mode"]
    [playlist-component]]])
