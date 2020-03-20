(ns cljs-karaoke.playback
  (:require [re-frame.core :as rf]
            [cljs.core.async :as async :refer [go go-loop <! >! chan]]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.subs.audio :as a-subs]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.songs :as songs]))

(defn play []
  (let [audio (rf/subscribe [::s/audio])
        effects-ready? (rf/subscribe [::a-subs/effects-audio-ready?])]
    (when-not @effects-ready?
      (let [effects-audio (rf/subscribe [::s/effects-audio])]
        (.play @effects-audio)
        (rf/dispatch [::audio-events/set-effects-audio-ready true])))
        ;; lyrics (rf/subscribe [::s/lyrics])]
    ;; (rf/dispatch [::views-events/set-current-view :playback])
    ;; (set! (.-currentTime @audio) 0)
    (.play @audio)
    (rf/dispatch [::events/play])))

(defn stop []
  (let [audio (rf/subscribe [::s/audio])]
        ;; current (rf/subscribe [::s/current-song])]
        ;; highlight-status (rf/subscribe [::s/highlight-status])
        ;; player-status (rf/subscribe [::s/player-status])
        ;; audio-events (rf/subscribe [::s/audio-events])
        ;; stop-channel (rf/subscribe [::s/stop-channel])]
    (when @audio
      (.pause @audio)
      (set! (.-currentTime @audio) 0))
    ;; (when @stop-channel
      ;; (async/put!  @stop-channel :stop))
    ;; (when @audio-events
      ;; (async/close! @audio-events))
    ;; (rf/dispatch-sync [::events/set-current-frame nil])
    ;; (rf/dispatch-sync [::events/set-lyrics nil])
    ;; (rf/dispatch-sync [::events/set-lyrics-loaded? false])
    ;; (when-not (nil? @player-status)
      ;; (async/close! @player-status))
    ;; (when-not (nil? @highlight-status)
      ;; (doseq [c @highlight-status]
        ;; (async/close! c))
    (rf/dispatch-sync [::events/set-playing? false])))
    ;; (rf/dispatch-sync [::events/set-highlight-status nil])
    ;; (rf/dispatch-sync [::events/set-player-status nil])
    ;; (rf/dispatch [::song-events/set-first-playback-position-updated? false])))
    ;; (songs/load-song @current)))

(defn pause []
  (let [audio (rf/subscribe [::s/audio])]
    (when @audio
      (.pause @audio))))

(def max-playback-rate 3.0)
(def min-playback-rate 0.1)

(defn ^:export update-playback-rate
  [delta]
  (let [rate (rf/subscribe [::s/audio-playback-rate])
        new-rate (+ @rate delta)
        new-rate (cond
                   (< new-rate min-playback-rate) min-playback-rate
                   (> new-rate max-playback-rate) max-playback-rate
                   :otherwise new-rate)]
    (rf/dispatch [::song-events/set-audio-playback-rate new-rate])))
