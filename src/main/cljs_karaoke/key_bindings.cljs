(ns cljs-karaoke.key-bindings
  (:require [re-frame.core :as rf]
            [keybind.core :as key]
            [mount.core :as mount :refer [defstate]]
            [cljs-karaoke.songs :as songs]
            [cljs-karaoke.playback :as playback :refer [play stop pause]]
            [cljs-karaoke.views.playback :refer [seek]]
            [cljs-karaoke.views.toasty :refer [trigger-toasty]]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.mobile :as mobile]
            [cljs-karaoke.views.cheatsheet :refer [show-cheatsheet]]))

(defn- handle-escape-key []
  (println "esc pressed!")
  (cond
    (not (empty? @(rf/subscribe [::s/modals]))) (rf/dispatch [::modal-events/modal-pop])
    :else
    (do
      (when-let [_ @(rf/subscribe [::s/loop?])]
        (rf/dispatch-sync [::events/set-loop? false]))
      (when  @(rf/subscribe [::s/song-playing?])
        (stop)))))

(defn- init-keybindings! []
  (println "setting up keybindings")
  (key/bind! "ctrl-space"
             ::ctrl-space-kb
             (fn []
               (println "ctrl-s pressed!")
               false))
  (key/bind! "esc" ::esc-kb handle-escape-key)
  (key/bind! "l r" ::l-r-kb #(songs/load-song))
  (key/bind! "alt-o" ::alt-o #(rf/dispatch [::views-events/set-view-property :playback :options-enabled? true]))
  (key/bind! "alt-h" ::alt-h #(rf/dispatch [::views-events/view-action-transition :go-to-home]))
  (key/bind! "left" ::left #(seek -10000.0))
  (key/bind! "right" ::right #(seek 10000.0))
  (key/bind! "meta-shift-l" ::loop-mode #(do
                                           (rf/dispatch [::events/set-loop? true])
                                           (rf/dispatch [::playlist-events/playlist-load])))
  (key/bind! "alt-shift-p" ::alt-meta-play #(play))
  (key/bind! "space" ::space-kb #(if @(rf/subscribe [::s/song-paused?]) (play) (pause)))
  (key/bind! "shift-right" ::shift-right #(do
                                            (stop)
                                            (rf/dispatch-sync [::playlist-events/playlist-next])))
  (key/bind! "t t" ::double-t #(trigger-toasty))
  (key/bind! "alt-r" ::alt-r #(rf/dispatch [::song-events/trigger-load-random-song]))
  (key/bind! "ctrl-shift-left" ::ctrl-shift-left #(rf/dispatch-sync [::song-events/inc-current-song-delay -250]))
  (key/bind! "ctrl-shift-right" ::ctrl-shift-right #(rf/dispatch-sync [::song-events/inc-current-song-delay 250]))
  (key/bind! "alt-shift-h" ::help #(show-cheatsheet)))

(defn disable-keybindings! []
  (key/disable!))

(defn enable-keybindings! []
  (key/enable!))

(defstate keybindings
  :start (when-not mobile/mobile?
           (init-keybindings!))
  :stop (key/unbind-all!))
