(ns cljs-karaoke.app
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs-karaoke.protocols :as protocols]
            [cljs-karaoke.songs :as songs :refer [song-table-component]]
            [cljs-karaoke.audio :as aud :refer [setup-audio-listeners]]
            [cljs-karaoke.remote-control :as remote-control]
            [cljs-karaoke.events.billboards :as billboard-events]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.common :as common-events]
            [cljs-karaoke.events.backgrounds :as bg-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.events.http-relay :as http-relay-events]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.modals :as modals :refer [show-export-sync-info-modal]]
            [cljs-karaoke.utils :as utils :refer [icon-button]]
            [cljs-karaoke.lyrics :as l :refer [preprocess-frames frame-text-string]]
            [cljs.reader :as reader]
            [cljs.core.async :as async :refer [go go-loop chan <! >! timeout alts!]]
            [stylefy.core :as stylefy]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as gevents]
            [goog.history.EventType :as EventType]
            [keybind.core :as key]
            [clojure.string :as str]
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
            [cljs-karaoke.notifications :as notifications]
            [cljs-karaoke.animation :refer [logo-animation]]
            [cljs-karaoke.svg.waveform :as waves]
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


(defn current-frame-display []
  (let [frame (rf/subscribe [::s/frame-to-display])]
    (when (and
           ((comp not nil?) @frame)
           (not (str/blank? (frame-text-string @frame))))
      [:div.current-frame
       [frame-text @frame]])))

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

   (when @(rf/subscribe [::s/display-home-button?])
     [:div.control
      [icon-button "home" "default" #(rf/dispatch [::views-events/set-current-view :home])]])
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
    [icon-button "random" "warning" #(rf/dispatch [::song-events/trigger-load-random-song])]]])

(defn playback-view []
  [:div.playback-view
   [spectro-overlay]

   [current-frame-display]
   (comment
    [:div.debug-view
      {:style {:background :white
               :border-radius "0.5em"}}
      (when-let [evt @(rf/subscribe [::s/next-lyrics-event])]
        [:p
         (str (:offset evt) " - " (:text evt))])
      (when-let [n @(rf/subscribe [::s/previous-frame])]
        [:p (protocols/get-text n)])
      (when-let [n @(rf/subscribe [::s/next-frame])]
        [:p (str (:offset n) " - " (protocols/get-text n))])
      (let [n @(rf/subscribe [::s/current-frame-done?])]
        [:p (if n "done" "not done")])])
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

(defn toasty []
  (when-let [toasty (rf/subscribe [::s/toasty?])]
    (when-let [_ (rf/subscribe [::s/initialized?])]
      [:div (stylefy/use-style
             (merge
              {:position   :fixed
               :bottom     "-474px"
               :left       "0"
               :opacity    0
               :z-index    2
               :display    :block
               :transition "all 0.5s ease-in-out"}
              (if @toasty {:bottom  "0px"
                           :opacity 1})))
       [:audio {:id    "toasty-audio"
                :src   "media/toasty.mp3"
                :style {:display :none}}]
       [:img {:src "images/toasty.png" :alt "toasty"}]])))

(defn trigger-toasty []
  (let [a (.getElementById js/document "toasty-audio")]
    (.play a)
    (rf/dispatch [::events/trigger-toasty])))

(defn app []
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
         :home [default-view]
         :playback [playback-view]
         :playlist [playlist-view-component]))])

(defn init-routing! []
  (secretary/set-config! :prefix "#")
  (defroute "/" []
    (println "home path")
    (rf/dispatch-sync [::playlist-events/playlist-load])
    (rf/dispatch-sync [::views-events/view-action-transition :go-to-home]))
  (defroute "/songs/:song"
    [song query-params]
    (println "song: " song)
    (println "query params: " query-params)
    (songs/load-song song)
    (when-some [offset (:offset query-params)]
      (rf/dispatch-sync [::events/set-lyrics-delay (long offset)]))
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
  (let [h (History.)]
    (gevents/listen h ^js EventType/NAVIGATE #(secretary/dispatch! (.-token ^js %)))
    (doto h (.setEnabled true))))

(defn get-sharing-url []
  (let [l js/location
        protocol (. l -protocol)
        host (. l -host)
        song @(rf/subscribe [::s/current-song])
        delay @(rf/subscribe [::s/custom-song-delay])]
    (->
     (str protocol "//" host "/#/songs/" song "?lyrics-delay=" delay)
     (js/encodeURI))))

(defn init-keybindings! []
  (key/bind! "ctrl-space"
             ::ctrl-space-kb
             (fn []
               (println "ctrl-s pressed!")
               false))
  (key/bind! "esc"
             ::esc-kb
             (fn []
               (println "esc pressed!")
               (cond
                 (not (empty? @(rf/subscribe [::s/modals]))) (rf/dispatch [::modal-events/modal-pop])
                 :else
                 (do
                   (when-let [_ @(rf/subscribe [::s/loop?])]
                     (rf/dispatch-sync [::events/set-loop? false]))
                   (when  @(rf/subscribe [::s/song-playing?])
                     (stop))))))
  (key/bind! "l r" ::l-r-kb #(songs/load-song))
  (key/bind! "alt-o" ::alt-o #(rf/dispatch [::views-events/set-view-property :playback :options-enabled? true]))
  (key/bind! "alt-h" ::alt-h #(rf/dispatch [::views-events/view-action-transition :go-to-home]))
  (key/bind! "left" ::left #(seek -10000.0))
  (key/bind! "right" ::right #(seek 10000.0))
  (key/bind! "meta-shift-l" ::loop-mode #(do
                                           (rf/dispatch [::events/set-loop? true])
                                           (rf/dispatch [::playlist-events/playlist-load])))
  (key/bind! "alt-shift-p" ::alt-meta-play #(play))
  (key/bind! "shift-right" ::shift-right #(do
                                            (stop)
                                            (rf/dispatch-sync [::playlist-events/playlist-next])))
  (key/bind! "t t" ::double-t #(trigger-toasty))
  (key/bind! "alt-x" ::alt-x #(remote-control/show-remote-control-id))
  (key/bind! "alt-s" ::alt-s #(remote-control/show-remote-control-settings))
  (key/bind! "alt-r" ::alt-r #(rf/dispatch [::song-events/trigger-load-random-song]))
  (key/bind! "ctrl-shift-left" ::ctrl-shift-left #(rf/dispatch [::song-events/inc-current-song-delay -250]))
  (key/bind! "ctrl-shift-right" ::ctrl-shift-right #(rf/dispatch [::song-events/inc-current-song-delay 250])))
(defn mount-components! []
  (reagent/render
   [app]
   (. js/document (getElementById "root"))))

(defn on-shake [evt] (trigger-toasty))

(defn init! []
  (println "init!")
  (mount-components!)
  (rf/dispatch-sync [::events/init-db])
  (init-routing!)
  (init-keybindings!)
  (when (ios?)
    (rf/dispatch-sync [::audio-events/set-audio-input-available? false])
    (rf/dispatch-sync [::audio-events/set-recording-enabled? false]))
  (. js/window (addEventListener "shake" on-shake false)))

(defn- capture-stream [^js/AudioBuffer audio]
  (cond
    (-> audio .-captureStream) (. audio (captureStream))
    (-> audio .-mozCaptureStream) (. audio (mozCaptureStream))
    :else  nil))

(defmethod aud/process-audio-event :canplaythrough
  [event]
  (println "handling canplaythrough event")
  (rf/dispatch-sync [::events/set-can-play? true])
  (let [audio @(rf/subscribe [::s/audio])
        output-mix @(rf/subscribe [::audio-subs/output-mix])
        ctx @(rf/subscribe [::audio-subs/audio-context])
        song-stream (capture-stream audio)
        song-paused? @(rf/subscribe [::s/song-paused?])]
    (rf/dispatch [::song-events/set-song-stream song-stream])))
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
    (rf/dispatch [::playlist-events/playlist-next])))

