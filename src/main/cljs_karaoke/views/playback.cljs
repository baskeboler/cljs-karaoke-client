(ns cljs-karaoke.views.playback
  (:require [re-frame.core :as rf]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.audio-input :refer [enable-audio-input-button spectro-overlay]]
            [stylefy.core :as stylefy]
            [cljs-karaoke.events.backgrounds :as bg-events]
            [cljs-karaoke.playback :as playback :refer [play pause stop]]
            [cljs-karaoke.styles :as styles
             :refer [time-display-style centered
                     top-left parent-style
                     top-right logo-bg-style]]
            [cljs-karaoke.utils :as utils :refer [icon-button]]
            [cljs-karaoke.events.playlists :as playlist-events]))
(defn lyrics-timing-progress []
  (let [time-remaining (rf/subscribe [::s/time-until-next-event])]
    ;; (fn []
    [:progress.progress.is-small.is-danger.lyrics-timing
     {:max 3000
      :value (- 3000 (if (> @time-remaining 3000) 3000 @time-remaining))}]))
(defn song-progress []
  (let [dur (rf/subscribe [::s/song-duration])
        cur (rf/subscribe [::s/song-position])]
    (fn []
      [:progress.progress.is-small.is-primary.song-progress
       {:max (if (number? @dur) @dur 0)
        :value (if (number? @cur) @cur 0)}
       (str (if (pos? @dur)
              (long (* 100 (/ @cur @dur)))
              0) "%")])))
(defn seek [offset]
  (let [audio            (rf/subscribe [::s/audio])
        pos              (rf/subscribe [::s/player-current-time])]
    ;; (when-not (nil? @player-status)
      ;; (async/close! @player-status)
    (set! (.-currentTime @audio) (+ @pos (/ (double offset) 1000.0)))))

(defn song-time-display [^double ms]
  (let [secs  (-> ms
                  (/ 1000.0)
                  (mod 60.0)
                  long)
        mins  (-> ms
                  (/ 1000.0)
                  (/ (* 60.0 1.0))
                  (mod 60.0)
                  long)
        hours (-> ms
                  (/ 1000.0)
                  (/ (* 60.0 60.0 1.0))
                  (mod 60.0)
                  long)]
    ;; (println  hours ":" mins ":" secs)
    [:div.time-display
     (stylefy/use-style time-display-style
                        (merge
                         {}
                         (if @(rf/subscribe [::audio-subs/recording?])
                           {:class "has-text-danger has-background-light"} {})))
     [:span.hours hours] ":"
     [:span.minutes mins] ":"
     [:span.seconds secs] "."
     [:span.milis (-> ms (mod 1000) long)]]))

(defn playback-controls []
  [:div.playback-controls.field.has-addons
   (stylefy/use-style top-right)
   [:div.control
    [enable-audio-input-button]]
   (when-not (= :playback @(rf/subscribe [::s/current-view]))
     [:div.control
      [icon-button "play" "primary" play]])
   (when @(rf/subscribe [::s/display-home-button?])
     [:div.control
      [icon-button "home" "default" #(rf/dispatch [::views-events/set-current-view :home])]])
   (when-not @(rf/subscribe [::s/song-paused?])
     [:div.control
      [icon-button "pause" "warning" pause]])
   (when-not @(rf/subscribe [::s/song-paused?])
     [:div.control
      [icon-button "stop" "danger" stop]])
   (when (and @(rf/subscribe [::audio-subs/audio-input-available?])
              @(rf/subscribe [::audio-subs/recording-enabled?]))
     [:div.control
      [icon-button "circle" "info" #(rf/dispatch [::audio-events/test-recording])
       (rf/subscribe [::audio-subs/recording-button-enabled?])]])
   [:div.control
    [icon-button "step-forward" "info" #(do
                                          (stop)
                                          (rf/dispatch [::playlist-events/playlist-next]))]]
   [:div.control
    [icon-button "random" "warning" #(rf/dispatch [::song-events/trigger-load-random-song])]]
   [:div.control
    [icon-button "trash" "danger" #(rf/dispatch [::bg-events/forget-cached-song-bg-image @(rf/subscribe [::s/current-song])])]]])
