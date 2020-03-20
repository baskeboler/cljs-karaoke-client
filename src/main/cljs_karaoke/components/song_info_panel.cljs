(ns cljs-karaoke.components.song-info-panel
  (:require [re-frame.core :as rf]
            [cljs-karaoke.subs :as s]))
            

(defn ^:export song-info-table []
  (let [current-song   (rf/subscribe [::s/current-song])
        lyrics-loaded? (rf/subscribe [::s/lyrics-loaded?])]
    [:table.table.is-fullwidth.is-narrow
     [:tbody
      [:tr
       [:td "current"]
       [:td
        (if-not (nil? @current-song)
          [:div.tag.is-info.is-normal
           @current-song]
          [:div.tag.is-danger.is-normal
           "no song selected"])]]
      [:tr
       [:td "is paused?"]
       [:td (if @(rf/subscribe [::s/song-paused?]) "yes" "no")]]
      [:tr
       [:td "lyrics loaded?"]
       [:td (if @lyrics-loaded?
              [:div.tag.is-success "loaded"]
              [:div.tag.is-danger "not loaded"])]]]]))
