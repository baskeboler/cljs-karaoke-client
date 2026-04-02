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
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.views.lyrics :refer [frame-text]]
            [cljs-karaoke.subs.http-relay :as relay-subs]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.components.menus :as menus :refer [menu-component]]
            [stylefy.core :as stylefy]
            [cljs-karaoke.audio-input :as audio-input :refer [enable-audio-input-button]]
            [cljs-karaoke.components.delay-select :refer [delay-select-component]]
            [cljs-karaoke.components.song-info-panel :refer [song-info-table]]
            [cljs-karaoke.components.dropdown :as dropdown]
            [cljs-karaoke.protocols :as protocols]))
(defn lyrics-view [lyrics]
  [:div.tile.is-child.is-vertical
   (for [frame (vec lyrics)]
     [:div [frame-text frame]])])

(defn toggle-song-list-btn []
  (let [visible? (rf/subscribe [::s/song-list-visible?])]
    [:button.button.is-fullwidth.is-outlined
     {:class        (concat
                     ["home-toolbar-btn"]
                     (if @visible?
                       ["is-success" "is-selected"]
                       ["is-primary"]))
      :title        (if @visible? "Hide song library" "Show song library")
      :aria-label   (if @visible? "Hide song library" "Show song library")
      :on-click     #(rf/dispatch [::song-list-events/toggle-song-list-visible])}
     [:span.icon
      (if @visible?
        [:i.fas.fa-eye-slash]
        [:i.fas.fa-eye])]
     [:span
      (if @visible?
        "Hide library"
        "Show library")]]))

(defn save-custom-delay-btn []
  (let [selected (rf/subscribe [::s/current-song])
        delay    (rf/subscribe [::s/lyrics-delay])]
    [:button.button.is-primary
     {:disabled (nil? @selected)
      :on-click #(when-not (nil? @selected)
                   (rf/dispatch [::events/set-custom-song-delay @selected @delay]))}
     "Save song delay"]))

(defn export-sync-data-btn []
  [:button.button.is-info
   {:on-click     (fn [_]
                    (modals/show-export-sync-info-modal))
    :title "Export sync data"
    :aria-label "Export sync data"}
   [:span.icon
    [:i.fas.fa-file-export]]
   [:span "Export"]])

(defn toggle-display-lyrics []
  (rf/dispatch [::events/toggle-display-lyrics]))

(defn toggle-display-lyrics-link []
  [:div.field>div.control
   [:a.button.is-info
    {:href     "#"
     :on-click toggle-display-lyrics}
    (if @(rf/subscribe [::s/display-lyrics?])
      "Hide lyrics"
      "Show lyrics")]])

(defn- control-panel-button-bar
  []
  (let [current-song            (rf/subscribe [::s/current-song])
        can-play?               (rf/subscribe [::s/can-play?])
        input-available?        (rf/subscribe [::audio-subs/audio-input-available?])]
    [:div.control-panel-button-bar
     [:button.button.is-primary
      {:on-click #(songs/load-song @current-song)}
       [:span.icon
        [:i.fas.fa-folder-open]]
       [:span "Load"]]
     [:button.button.is-info
      (if @can-play?
        {:on-click     play
         :title "Play current song"
         :aria-label "Play current song"}
        {:disabled true})
       [:span.icon
        [:i.fas.fa-play]]
      [:span "Play"]]
     [:button.button.is-warning.stop-btn
      {:on-click     stop
       :title "Stop playback"
       :aria-label "Stop playback"}
       [:span.icon
        [:i.fas.fa-stop]]
      [:span "Stop"]]
     [export-sync-data-btn]
     (when @input-available?
       [enable-audio-input-button])]))

(defn playback-controls-panel []
  [:section.playback-controls-panel
   [toggle-display-lyrics-link]
   [delay-select-component]
   [song-info-table]
   [:div.playback-controls-panel-actions
    [control-panel-button-bar]
    [save-custom-delay-btn]]
   [audio-input/audio-viz]])


(defn- toggle-menu-btn []
  [:button.button.is-small.is-outlined.is-primary.toggle-tools-btn
   (stylefy/use-style
    {:margin-bottom "0.5em"}
    {:on-click #(rf/dispatch [::views-events/set-view-property :home :display-menu?
                              (not @(rf/subscribe [::s/view-property :home :display-menu?]))])})
   [:span.icon
    [:i.fas.fa-sliders-h]]
   [:span
    (if @(rf/subscribe [::s/view-property :home :display-menu?])
      "Hide tools"
      "Show tools")]])

(def control-panel-style
  {:transition "all 0.8s ease-out"})

(defn a-dropdown-menu []
  (protocols/render-component
   (dropdown/dropdown-menu-button
     [:i.fa.fa-ellipsis-v]
     [(dropdown/url-item "item 1" "#")
      (dropdown/url-item "item 2" "#")])))

(defn control-panel []
  (let [lyrics             (rf/subscribe [::s/lyrics])
        display-menu?      (rf/subscribe [::s/view-property :home :display-menu?])
        display-lyrics?    (rf/subscribe [::s/display-lyrics?])
        current-song       (rf/subscribe [::s/current-song])
        lyrics-loaded?     (rf/subscribe [::s/lyrics-loaded?])
        song-list-visible? (rf/subscribe [::s/song-list-visible?])
        input-available?   (rf/subscribe [::audio-subs/audio-input-available?])
        can-play?          (rf/subscribe [::s/can-play?])
        song-paused?       (rf/subscribe [::s/song-paused?])
        lyrics-delay       (rf/subscribe [::s/lyrics-delay])]
    [:div.control-panel
     (stylefy/use-style
      default-page-styles
      {:class (if @(rf/subscribe [::s/song-paused?])
                ["song-paused"]
                ["song-playing"])})

     [:section.home-hero
      [:div.home-hero-copy
       [:p.home-kicker "Karaoke Party"]
       [:h1.title.is-2 "Control Center"]
       [:p.home-hero-summary
        (if @current-song
          "Pick a song, fine-tune the sync, and jump into playback."
          "Start by choosing a song from the library below, then enable audio when you are ready.")]]
      [:div.home-current-song
       [:p.home-section-label "Selected song"]
       (if @current-song
         (let [{:keys [title artist]} (songs/song-display-data @current-song)]
           [:div
            [:p.home-current-song-title title]
            (when artist
              [:p.home-current-song-artist artist])])
         [:p.home-current-song-empty "No song selected yet."])
       [:div.home-status-pills
        [:span.tag.is-light
         (if @lyrics-loaded? "Lyrics ready" "Lyrics pending")]
        [:span.tag.is-light
         (if @can-play? "Audio ready" "Audio pending")]
        [:span.tag.is-light
         (if @song-paused? "Paused" "Playing")]]
       (when @current-song
         [:p.home-delay-copy
          (str "Current lyrics delay: " @lyrics-delay " ms")])]]

     [:section.home-primary.columns.is-variable.is-5
      [:div.column.is-8-desktop
       [:div.home-workspace
        [:div.home-section-heading
         [:div
          [:h2.title.is-4 "Playback workspace"]
          [:p "Operate the current song, control sync, and manage audio input without leaving this page."]]]
        [playback-controls-panel]]]
      [:div.column.is-4-desktop
       [:div {:class (concat ["home-tools"]
                             (when-not @display-menu?
                               ["is-collapsed"]))}
        [:div.home-section-heading
         [:div
          [:h2.title.is-5 "Tools"]
          [:p "Secondary playback utilities and shortcuts."]]
         [toggle-menu-btn]]
        (if @display-menu?
          [menu-component]
          [:p.home-tools-collapsed-copy
           "Open the tools menu for playlist utilities, sharing, and timing shortcuts."])]]]

     [:section.home-library
      [:div.home-section-heading
       [:div
        [:h2.title.is-4 "Song library"]
        [:p "Search the catalog and launch tracks directly into playback."]]]
      [:div.home-library-toolbar
       [toggle-song-list-btn]]
      (when @song-list-visible?
        [song-table-component])]]))
