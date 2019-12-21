(ns cljs-karaoke.remote-control
  (:require [re-frame.core :as rf :include-macros true]
            [cljs.core.async :as async :refer [go go-loop >! <! chan]]
            [reagent.core :as reagent :refer [atom]]
            [cljs-karaoke.utils :as utils]
            [cljs-karaoke.modals :as modals :refer [modal-card-dialog]]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.events.http-relay :as remote-events]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.subs.http-relay :as relay-subs]
            [cljs-karaoke.remote-control.commands :as cmds]
            [cljs-karaoke.remote-control.execute]
            [cljs-karaoke.remote-control.queue]
            [cljs-karaoke.remote-control.queue-processor]))
(def play-song-command cmds/play-song-command)
(def stop-command cmds/stop-command)
(def playlist-next-command cmds/playlist-next-command)

(defn close-modal-button []
  [:button.button.is-danger
   {:on-click #(rf/dispatch [::modal-events/modal-pop])}
   "Close"])

(defn show-remote-control-id []
  (let [listener-id @(rf/subscribe [::relay-subs/http-relay-listener-id])
        modal (modal-card-dialog
               {:title "Local remote control ID"
                :content [:div.remote-control-info-content
                          [:div.field
                           [:div.control
                            [:input.input.is-static.is-large.is-danger
                             {:id "remote-control-id"
                              :value listener-id
                              :read-only true}]]
                           [:span.help "Use this code to control this screen remotely from another browser window."]]]
                :footer [close-modal-button]})]
    (rf/dispatch [::modal-events/modal-push modal])))

(defn remote-control-settings []
  (let [value (atom  "")]
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
                :footer [close-modal-button]})]
    (rf/dispatch [::modal-events/modal-push modal])))

(defn remote-playlist-next-button []
  [:button.button.is-primary
   {:on-click #(rf/dispatch [::remote-events/remote-control-command (playlist-next-command)])}
   [:span.icon
    [:i.fa.fa-step-forward]]])

(defn remote-stop-button []
  [:button.button.is-danger
   {:on-click #(rf/dispatch [::remote-events/remote-control-command (stop-command)])}
   [:span.icon>i.fa.fa-stop]])
(defn remote-control-component []
  [:div.columns>div.column.is-12
   [:p.title "remote control"]
   [:div.field.has-addons
    [:div.control
     [remote-stop-button]]
    [:div.control
     [remote-playlist-next-button]]]])

    
