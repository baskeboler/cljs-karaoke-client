(ns cljs-karaoke.components.stats-dialog
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-karaoke.components.stats :refer [stats-item stats-row]]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.modals :as modals]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.lyrics :refer [frames-chart]]
            [goog.string :refer [format]]))



(defn song-stats []
  [:div
   [stats-row
     [stats-item "frames" @(rf/subscribe [::subs/current-song-frame-count])]
     [stats-item "words" @(rf/subscribe [::subs/current-song-word-count])]
     [stats-item "avg words per frame" (format "%1.1f"
                                              @(rf/subscribe [::subs/current-song-avg-words-frame]))]]
   [:div.columns
    [:div.column.has-text-centered
     [frames-chart @(rf/subscribe [::subs/playback-song])]]]])

(defn ^:export show-stats-dialog []
  (modals/show-modal-card-dialog
   {:title "song stats"
    :content [song-stats]
    :footer [modals/footer-buttons]}))
    
