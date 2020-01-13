(ns cljs-karaoke.views.control-panel
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [cljs-karaoke.playback :refer [play stop]]
            [cljs-karaoke.utils :as utils]
            [cljs-karaoke.modals :as modals]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.styles :refer [default-page-styles]]
            [cljs-karaoke.songs :as songs :refer [song-table-component]]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.http-relay :as relay-events]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.views.lyrics :refer [frame-text]]
            [cljs-karaoke.remote-control :as remote-control]
            [cljs-karaoke.subs.http-relay :as relay-subs]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.components.menus :as menus :refer [menu-component]]
            [stylefy.core :as stylefy]
            [cljs-karaoke.audio-input :as audio-input :refer [enable-audio-input-button]]))

(defn lyrics-view [lyrics]
  [:div.tile.is-child.is-vertical
   (for [frame (vec lyrics)]
     [:div [frame-text frame]])])

(defn toggle-song-list-btn []
  (let [visible? (rf/subscribe [::s/song-list-visible?])]
    [:button.button.is-fullwidth.tooltip
     {:class        (concat []
                            (if @visible?
                              ["is-selected"
                               "is-success"]
                              ["is-danger"]))
      :data-tooltip "TOGGLE SONG LIST"
      :on-click     #(rf/dispatch [::song-list-events/toggle-song-list-visible])}
     [:span.icon
      (if @visible?
        [:i.fas.fa-eye-slash];"Hide songs"
        [:i.fas.fa-eye])]])) ;"Show song list")]]))
(defn save-custom-delay-btn []
  (let [selected (rf/subscribe [::s/current-song])
        delay    (rf/subscribe [::s/lyrics-delay])]
    [:button.button.is-primary
     {:disabled (nil? @selected)
      :on-click #(when-not (nil? @selected)
                   (rf/dispatch [::events/set-custom-song-delay @selected @delay]))}
     "remember song delay"]))

(defn export-sync-data-btn []
  [:button.button.is-info.tooltip
   {:on-click     (fn [_]
                    (modals/show-export-sync-info-modal))
    :data-tooltip "EXPORT SYNC INFO"}
   [:span.icon
    [:i.fas.fa-file-export]]])
    ;; "export sync data"]])

(defn enable-remote-control-btn []
  [:button.button.is-info.tooltip
   {:on-click     (fn [_]
                    (if-not @(rf/subscribe [::relay-subs/http-relay-listener-id])
                      (rf/dispatch-sync [::relay-events/init-http-relay-listener]))
                    (remote-control/show-remote-control-id))
    :data-tooltip "Remote Control information"}
   [:span.icon
    [:i.fas.fa-wifi]]])

(defn camera-btn []
  [:button.button
   {:on-click     (fn [_]
                    (if-not @(rf/subscribe [::relay-subs/http-relay-listener-id])
                      (rf/dispatch-sync [::relay-events/init-http-relay-listener]))
                    (remote-control/show-remote-control-id))
    :data-tooltip "Enabled camera"}
   [:span.icon>i.fa.fa-camera]])

(defn remote-control-btn []
  [:button.button.is-info.tooltip
   {:on-click     (fn [_]
                    (remote-control/show-remote-control-settings))
    :data-tooltip "Control Remote Karaoke"}
   [:span.icon
    [:i.fas.fa-satellite-dish]]])
(defn info-table []
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

(defn delay-select []
  (let [delay (rf/subscribe [::s/lyrics-delay])]
    [:div.field
     [:div.control
      [:div.select.is-primary.is-fullwidth.delay-select
       [:select {:value     @delay
                 :on-change #(rf/dispatch [::events/set-lyrics-delay (-> % .-target .-value (long))])}
        (for [v (vec (range -10000 10001 250))]
          [:option {:key   (str "opt_" v)
                    :value v}
           v])]]]]))
(defn toggle-display-lyrics []
  (rf/dispatch [::events/toggle-display-lyrics]))

(defn toggle-display-lyrics-link []
  [:div.field>div.control
   [:a.button.is-info
    {:href     "#"
     :on-click toggle-display-lyrics}
    (if @(rf/subscribe [::s/display-lyrics?])
      "hide lyrics"
      "show lyrics")]])

(defn- control-panel-button-bar
  []
  (let [current-song            (rf/subscribe [::s/current-song])
        can-play?               (rf/subscribe [::s/can-play?])
        input-available?        (rf/subscribe [::audio-subs/audio-input-available?])]
    [:div.field.has-addons.buttons.are-small
     [:div.control
      [:button.button.is-primary {:on-click #(songs/load-song @current-song)}
       [:span.icon
        [:i.fas.fa-folder-open]]]]
     [:div.control
      [:button.button.is-info.tooltip
       (if @can-play?
         {:on-click     play
          :data-tooltip "PLAY"}
         {:disabled true})
       [:span.icon
        [:i.fas.fa-play]]]]
     [:div.control
      [:button.button.is-warning.stop-btn.tooltip
       {:on-click     stop
        :data-tooltip "STOP"}
       [:span.icon
        [:i.fas.fa-stop]]]]
     [:div.control
      [export-sync-data-btn]]
     [:div.control
      [toggle-song-list-btn]]
     [:div.control
      [enable-remote-control-btn]]
     [:div.control
      [remote-control-btn]]
     (when @input-available?
       [:div.control
        [enable-audio-input-button]])]))

(defn playback-controls-panel []
  (let [remote-control-enabled? (rf/subscribe [::relay-subs/remote-control-enabled?])]
    [:div.card>div.card-content
     [toggle-display-lyrics-link]
     [delay-select]
     [info-table]
     [control-panel-button-bar]
     [:div.field
      [:div.control
       [save-custom-delay-btn]]]
     (when @remote-control-enabled?
       [remote-control/remote-control-component])
     [audio-input/audio-viz]]))

;; (defn show-playback-controls-modal []
  ;; (common))

(defn- toggle-menu-btn []
  [:button.button.is-small.is-outlined.is-primary
   (stylefy/use-style
    {:margin-bottom "0.5em"}
    {:on-click #(rf/dispatch [::views-events/set-view-property :home :display-menu?
                              (not @(rf/subscribe [::s/view-property :home :display-menu?]))])})
   "toggle menu"])

(defn control-panel []
  (let [lyrics                  (rf/subscribe [::s/lyrics])
        display-menu?           (rf/subscribe [::s/view-property :home :display-menu?])
        display-lyrics?         (rf/subscribe [::s/display-lyrics?])
        current-song            (rf/subscribe [::s/current-song])
        lyrics-loaded?          (rf/subscribe [::s/lyrics-loaded?])
        song-list-visible?      (rf/subscribe [::s/song-list-visible?])
        remote-control-enabled? (rf/subscribe [::relay-subs/remote-control-enabled?])
        input-available?        (rf/subscribe [::audio-subs/audio-input-available?])]
    [:div.control-panel
     (stylefy/use-style
      default-page-styles
      {:class (if @(rf/subscribe [::s/song-paused?])
                ["song-paused"]
                ["song-playing"])})

     [:div.columns>div.column.is-12>h1.title.is-2 "Control Panel"]
     ;; [:div.columns>div.column.is-12 (stylefy/use-style {:background-color "rgba(1,1,1, .3)"})
     [:div.columns
       (when @display-menu?
         [:div.column.is-3>div.box
          [toggle-menu-btn]
          [menu-component]])
      (when @song-list-visible?
        [:div.column>div.box
         (when-not @display-menu?
           [toggle-menu-btn])
         [song-table-component]])]]))

#_[:div.column
   [playback-controls-panel]]
;; [audio-input/test-viz]]]
#_(when @display-lyrics?
    [:div.column (stylefy/use-style {:background-color "rgba(1,1,1, .3)"})
     [lyrics-view @lyrics]])
