(ns cljs-karaoke.remote-control.execute
  (:require [cljs.core.async :as async :refer [<! >! go go-loop]]
            [re-frame.core :as rf]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.notifications :refer [add-notification notification]]
            [cljs-karaoke.playback :refer [play stop]]))
(defmulti execute-command
  "execute remote control commands"
  (fn [cmd]
    (let [cmd-type (:command cmd)]
      (if (string? cmd-type)
        (keyword cmd-type)
        cmd-type))))

(defmethod execute-command :default
  [cmd]
  (println "Unknown remote control command: " cmd))

(defmethod execute-command :play-song
  [cmd]
  (cljs-karaoke.songs/load-song (:song cmd))
  (go-loop [_ (<! (async/timeout 2500))]
    (if @(rf/subscribe [:cljs-karaoke.subs/can-play?])
      (play)
      (recur (<! (timeout 500)))))
  (add-notification (str "Remote control play song: " (:song cmd))))

(defmethod execute-command :stop
  [cmd]
  (stop)
  (add-notification (notification :warning "Stopped by remote control")))

(defmethod execute-command :playlist-next
  [cmd]
  (stop)
  (rf/dispatch [::playlist-events/playlist-next])
  (add-notification (notification :warning "Skipped to next in playlist by remote control")))


