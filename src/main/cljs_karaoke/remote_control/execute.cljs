(ns cljs-karaoke.remote-control.execute
  (:require [cljs.core.async :as async :refer [<! >! go go-loop]]
            [re-frame.core :as rf]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.notifications :refer [add-notification]]))

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
      (cljs-karaoke.playback/play)
      (recur (<! (timeout 500)))))
  (add-notification (str "Remote control play song: " (:song cmd))))

(defmethod execute-command :stop
  [cmd]
  (cljs-karaoke.playback/stop))

(defmethod execute-command :playlist-next
  [cmd]
  (cljs-karaoke.playback/stop)
  (rf/dispatch [::playlist-events/playlist-next]))


