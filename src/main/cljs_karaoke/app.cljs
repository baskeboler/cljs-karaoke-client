(ns cljs-karaoke.app
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [day8.re-frame.http-fx]
            [cljs.reader :as reader]
            [cljs.core.async :as async :refer [go go-loop chan <! >! timeout alts!]]
            [stylefy.core :as stylefy]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as gevents]
            [goog.history.EventType :as EventType]
            [keybind.core :as key]
            [clojure.string :as str]
            [ajax.core :as ajax]
            [cljs-karaoke.protocols :as protocols]
            [cljs-karaoke.songs :as songs :refer [song-table-component]]
            [cljs-karaoke.audio :as aud :refer [setup-audio-listeners]]
            [cljs-karaoke.remote-control :as remote-control]
            [cljs-karaoke.events.common :as common-events]
            [cljs-karaoke.events.billboards :as billboard-events]
            [cljs-karaoke.events.backgrounds :as bg-events]
            [cljs-karaoke.events.lyrics :as lyrics-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.editor :as editor-events]
            [cljs-karaoke.events.metrics :as metrics-events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.events.user :as user-events]
            [cljs-karaoke.events.http-relay :as http-relay-events]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.events.notifications :as notifications-events]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.modals :as modals :refer [show-export-sync-info-modal]]
            [cljs-karaoke.utils :as utils :refer [icon-button]]
            [cljs-karaoke.lyrics :as l :refer [preprocess-frames frame-text-string]]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.audio-input :refer [enable-audio-input-button spectro-overlay]]
            [cljs-karaoke.playback :as playback :refer [play stop]]
            [cljs-karaoke.remote-control :as remote-control]
            [cljs-karaoke.views.billboards :refer [billboards-component]]
            [cljs-karaoke.views.page-loader :as page-loader]
            [cljs-karaoke.views.seek-buttons :as seek-buttons :refer [right-seek-component]]
            [cljs-karaoke.views.control-panel :refer [control-panel]]
            [cljs-karaoke.views.lyrics :refer [frame-text]]
            [cljs-karaoke.views.playlist-mode :refer [playlist-view-component]]
            [cljs-karaoke.views.navbar :as navbar]
            [cljs-karaoke.views.editor  :refer [editor-component]]
            [cljs-karaoke.views.playback :refer [playback-controls lyrics-timing-progress song-progress seek song-time-display]]
            [cljs-karaoke.views.toasty  :as toasty-views :refer [toasty trigger-toasty]]
            [cljs-karaoke.notifications :as notifications]
            ;; [cljs-karaoke.animation :refer [logo-animation]]
            [cljs-karaoke.svg.waveform :as waves]
            [cljs-karaoke.key-bindings :refer [init-keybindings!]]
            [cljs-karaoke.styles :as styles
             :refer [time-display-style centered
                     top-left parent-style
                     top-right logo-bg-style]]
            ["shake.js" :as Shake])
  (:require-macros [cljs-karaoke.embed :refer [inline-svg]])
  (:import goog.History))

(stylefy/init)

(defn- ios? []
  (-> (. js/navigator -platform)
      (str/lower-case)
      (str/includes? "ios")))

(defonce shake (Shake. (clj->js {:threshold 15 :timeout 1000})))

(.start shake)

(defonce my-shake-event (Shake. (clj->js {:threshold 15 :timeout 1000})))

(def bg-style (rf/subscribe [::s/bg-style]))



(defn current-frame-display []
  (let [frame (rf/subscribe [::s/frame-to-display])]
    (when (and
           @(rf/subscribe [::s/song-playing?])
           ((comp not nil?) @frame)
           (not (str/blank? (frame-text-string @frame))))
      [:div.current-frame
       [frame-text @frame]])))


(defn playback-debug-panel []
  [:div.debug-view
   {:style {:background :white
            :border-radius "0.5em"}}
   (when-let [t @(rf/subscribe [::s/time-until-next-event])]
     [:p (str "time until: "
              t)])
   (when-let [evt @(rf/subscribe [::s/next-lyrics-event])]
     [:p
      (str "next event: " (:offset evt) " - " (:text evt))])
   (when-let [n @(rf/subscribe [::s/previous-frame])]
     [:p (str "previous frame: "
              (protocols/get-text n))])
   (when-let [n @(rf/subscribe [::s/current-frame])]
     [:p (str "current frame: "(:offset n) " - " (protocols/get-text n))])
   (when-let [n @(rf/subscribe [::s/frame-to-display])]
     [:p (str "displayed frame: "(:offset n) " - " (protocols/get-text n))])
   (when-let [n @(rf/subscribe [::s/next-frame])]
     [:p (str "next frame: "(:offset n) " - " (protocols/get-text n))])
   (let [n @(rf/subscribe [::s/current-frame-done?])]
     [:p (if n "done" "not done")])])

(defn playback-view []
  [:div.playback-view
   [spectro-overlay]
   [current-frame-display]
   (comment)
   #_[playback-debug-panel]
   [song-time-display (* 1000 @(rf/subscribe [::s/song-position]))]
   [billboards-component]
   (when (and
          @(rf/subscribe [::s/song-paused?])
          @(rf/subscribe [::s/can-play?]))
     [:div
      (when @(rf/subscribe
              [::s/view-property :playback :options-enabled?])
        [:a
         (stylefy/use-style
          top-left
          {:on-click #(rf/dispatch [::views-events/set-current-view :home])})
         [:span.icon
          [:i.fas.fa-cog.fa-3x]]])
      [:a
       (stylefy/use-style
        (merge
         centered
         {:z-index 500})
        {:on-click play})
       [:span.icon
        [:i.fas.fa-play.fa-5x
         (stylefy/use-style {:text-shadow "0px 0px 9px white"})]]]])
   (when-not @(rf/subscribe [::s/can-play?])
     [:a
      (stylefy/use-style
       centered
       {:on-click
        #(if-let [song @(rf/subscribe [::s/current-song])]
           (songs/load-song song)
           (songs/load-song))})
      [:span.icon
       [:i.fas.fa-sync.fa-5x]]])
   ^{:class "edge-stop-btn"} [playback-controls]
   [seek-buttons/seek-component #(seek 10000) #(seek -10000)]
   (when-not @(rf/subscribe [::s/song-paused?])
     [:div.edge-progress-bar
      [lyrics-timing-progress]
      [song-progress]])])

(defn default-view []
  [:div.default-view.container-fluid
   [control-panel]
   [:button.button.is-danger.edge-stop-btn
    {:class (if @(rf/subscribe [::s/song-paused?])
              []
              ["song-playing"])
     :on-click stop}
    [:span.icon
     [:i.fas.fa-stop]]]
   (when-not @(rf/subscribe [::s/song-paused?])
     [:div.edge-progress-bar
      [song-progress]])])


(defn app
  "main app component"
  []
  [:div.app
   (when @(rf/subscribe [::s/navbar-visible?])
     [navbar/navbar-component])
   [toasty]
   [notifications/notifications-container-component]
   [modals/modals-component]
   [page-loader/page-loader-component]
   [:div.app-bg (stylefy/use-style (merge (parent-style) @bg-style))]
   ;; [logo-animation]
   ;; [:div.page-content.roll-in-blurred-top
   (when-let [_ (and
                 @(rf/subscribe [::s/initialized?])
                 @(rf/subscribe [::s/current-view]))]
       (condp = @(rf/subscribe [::s/current-view])
         :home     [default-view]
         :playback [playback-view]
         :playlist [playlist-view-component]
         :editor   [editor-component]))])

(defn ^:export load-song-global [s]
  (songs/load-song s))

(defn init-routing! []
  (secretary/set-config! :prefix "#")
  (defroute "/" []
    (println "home path")
    ;; (rf/dispatch-sync [::playlist-events/playlist-load])
    (rf/dispatch-sync [::views-events/view-action-transition :go-to-home]))
  (defroute "/songs/:song"
    [song query-params]
    (println "song: " song)
    (println "query params: " query-params)
    ;; (rf/dispatch [::events/set-pageloader-active? true])
    (rf/dispatch [::events/set-pageloader-exiting? false])
    (songs/load-song song)
    (if-some [offset (:offset query-params)]
      (rf/dispatch-sync [::events/set-lyrics-delay (long offset)])
      (rf/dispatch [::song-events/update-song-hash song]))
    ;; (rf/dispatch [::events/set-custom-song-delay song (long offset)]))
    (when-some [show-opts? (:show-opts query-params)]
      (rf/dispatch-sync [::views-events/set-view-property :playback :options-enabled? true])))

  ;; Quick and dirty history configuration.
  (defroute "/party-mode" []
    (println "fuck yea! party mode ON")
    (rf/dispatch [::playlist-events/set-loop? true])
    (rf/dispatch [::playlist-events/playlist-load]))
  (defroute "/playlist" []
    (rf/dispatch-sync [::views-events/set-current-view :playlist]))
  (defroute "/editor" []
    (rf/dispatch [::views-events/view-action-transition :go-to-editor]))
  (let [h (History.)]
    (gevents/listen h ^js EventType/NAVIGATE #(secretary/dispatch! (.-token ^js %)))
    (doto h (.setEnabled true))))

(defn get-sharing-url []
  (let [l        js/location
        protocol (. l -protocol)
        host     (. l -host)
        song     @(rf/subscribe [::s/current-song])
        delay    @(rf/subscribe [::s/custom-song-delay])]
    (->
     (str protocol "//" host "/#/songs/" song "?lyrics-delay=" delay)
     (js/encodeURI))))


(defn mount-components! []
  (reagent/render
   [app]
   (. js/document (getElementById "root"))))

(defn on-shake [evt] (trigger-toasty))

(defn init! []
  (println "init!")
  (rf/dispatch-sync [::events/init-db])
  (mount-components!)
  (init-routing!)
  (init-keybindings!)
  (when (ios?)
    (rf/dispatch-sync [::audio-events/set-audio-input-available? false])
    (rf/dispatch-sync [::audio-events/set-recording-enabled? false]))
  (. js/window (addEventListener "shake" on-shake false)))

(defn ^:dev/after-load start-app []
  (println "start app, mounting components")
  (mount-components!))
  

(defn ^:dev/before-load stop-app []
  (println "stop app"))


(defn- capture-stream [^js/AudioBuffer audio]
  (cond
    (-> audio .-captureStream) (. audio (captureStream))
    (-> audio .-mozCaptureStream) (. audio (mozCaptureStream))
    :else  nil))

(defmethod aud/process-audio-event :canplaythrough
  [event]
  (println "handling canplaythrough event")
  (let [audio @(rf/subscribe [::s/audio])
        output-mix @(rf/subscribe [::audio-subs/output-mix])
        ctx @(rf/subscribe [::audio-subs/audio-context])
        song-stream (capture-stream audio)
        song-paused? @(rf/subscribe [::s/song-paused?])]
    (rf/dispatch-sync [::song-events/set-song-stream song-stream])
    (rf/dispatch-sync [::events/set-can-play? true])))
    ;; (play)))
(defn ->ms [secs]
  (* 1000 secs))
(defn ->secs [ms]
  (/ (double ms) 1000.0))


(defmethod aud/process-audio-event :timeupdate
  [event]
  (when-let [a @(rf/subscribe [::s/audio])]
    (rf/dispatch-sync [::events/set-player-current-time (.-currentTime a)])))

(defmethod aud/process-audio-event :playing
  [event]
  (println "playing event")
  (rf/dispatch-sync [::events/set-song-duration (.-duration @(rf/subscribe [::s/audio]))])
  (rf/dispatch-sync [::events/set-playing? true]))

(defmethod aud/process-audio-event :pause
  [event]
  (rf/dispatch-sync  [::events/set-playing? false]))

(defmethod aud/process-audio-event :ended
  [event]
  (println "processing ended event: " event)
  (rf/dispatch-sync [::events/set-playing? false])
  (when-let [_ @(rf/subscribe [::s/loop?])]
    (rf/dispatch-sync [::playlist-events/playlist-next])))

