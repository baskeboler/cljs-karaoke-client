(ns cljs-karaoke.components.song-info-panel
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [cljs-karaoke.subs :as s]))

(defn display-song-data [name]
  (let [clean-name     (-> name
                           (str/replace #"_" " ")
                           (str/replace #"\s+" " ")
                           (str/trim))
        [artist title] (str/split clean-name #"-" 2)]
    (if (str/blank? title)
      {:title clean-name
       :artist nil}
      {:title (str/trim title)
       :artist (str/trim artist)})))
            

(defn ^:export song-info-table []
  (let [current-song   (rf/subscribe [::s/current-song])
        lyrics-loaded? (rf/subscribe [::s/lyrics-loaded?])]
    [:div.song-info-panel
     [:div.song-info-row
      [:p.song-info-label "Current"]
      [:div.song-info-value
       (if-not (nil? @current-song)
         (let [{:keys [title artist]} (display-song-data @current-song)]
           [:div
            [:div.tag.is-info.is-normal title]
            (when artist
              [:p.song-info-artist artist])])
         [:div.tag.is-danger.is-normal "No song selected"])]]
     [:div.song-info-row
      [:p.song-info-label "Playback"]
      [:div.song-info-value
       [:span.tag.is-light
        (if @(rf/subscribe [::s/song-paused?]) "Paused" "Playing")]]]
     [:div.song-info-row
      [:p.song-info-label "Lyrics"]
      [:div.song-info-value
       (if @lyrics-loaded?
         [:span.tag.is-success.is-light "Loaded"]
         [:span.tag.is-danger.is-light "Not loaded"])]]]))
