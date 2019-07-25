(ns cljs-karaoke.remote-control
  (:require [re-frame.core :as rf :include-macros true]
            [reagent.core :as reagent]
            [cljs-karaoke.utils :as utils :refer [modal-card-dialog]]
            [cljs-karaoke.events.playlists :as playlist-events]
            ;; [cljs-karaoke.songs :as songs]
            [cljs-karaoke.events.modals :as modal-events]
            ;; [cljs-karaoke.playback :as playback]
            [cljs-karaoke.subs.http-relay :as relay-subs]
            [cljs.core.async :as async :refer [go go-loop >! <! chan]]))

(defn ^export generate-remote-control-id []
  (->> (random-uuid)
       str
       (take 8)
       (apply str)))

(defn ^export play-song-command [song]
  {:command :play-song
   :song song})

(defn ^export stop-command []
  {:command :stop})

(defn ^export playlist-next-command []
  {:command :playlist-next})

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

(defn show-remote-control-id []
  (let [listener-id @(rf/subscribe [::relay-subs/http-relay-listener-id])
        modal (modal-card-dialog
               {:title "Remote control info"
                :content [:div.remote-control-info-content
                          [:div.field>div.control
                           [:textarea.textarea.is-primary
                            {:id "remote-control-id"
                             :value listener-id
                             :read-only true}]]]
                :footer nil})]
    (rf/dispatch [::modal-events/modal-push modal])))

(defn remote-control-settings []
  (let [value (reagent/atom  "")]
    [:div.remote-control-settings-content
     [:div.field>div.control
      [:input.input.is-primary
       {:id "remote-control-id"
        :type :text
        :placeholder "Enter the connection code for the remote karaoke"
        :value @(rf/subscribe [::relay-subs/remote-control-id])
        :on-change #(do
                      (rf/dispatch  [:cljs-karaoke.events.http-relay/set-remote-control-id (-> % .-target .-value)]))}]]]))

(defn show-remote-control-settings []
  (let [modal (modal-card-dialog
               {:title "Set the remote screen id"
                :content [remote-control-settings]
                :footer nil})]
    (rf/dispatch [::modal-events/modal-push modal])))

(defmethod execute-command :play-song
  [cmd]
  (cljs-karaoke.songs/load-song (:song cmd))
  (go-loop [_ (<! (async/timeout 2500))]
    (if @(rf/subscribe [:cljs-karaoke.subs/can-play?])
      (cljs-karaoke.playback/play)
      (recur (<! (timeout 500))))))

(defmethod execute-command :stop
  [cmd]
  (cljs-karaoke.playback/stop))

(defmethod execute-command :playlist-next
  [cmd]
  (cljs-karaoke.playback/stop)
  (rf/dispatch [::playlist-events/playlist-next]))

(defmethod execute-command "play-song"
  [cmd]
  (cljs-karaoke.songs/load-song (:song cmd))
  (play))

(defmethod execute-command "stop"
  [cmd]
  (cljs-karaoke.playback/stop))

(defmethod execute-command "playlist-next"
  [cmd]
  (cljs-karaoke.playback/stop)
  (rf/dispatch [::playlist-events/playlist-next]))

(defonce remote-commands-queue (chan))

(go-loop [next-command (<! remote-commands-queue)]
  (execute-command next-command)
  (recur (<! remote-commands-queue)))

(defn queue-remote-command [cmd]
  (go
    (>! remote-commands-queue cmd)))

(println "Remote control queue initiated.")
