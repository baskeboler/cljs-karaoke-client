(ns cljs-karaoke.app
  (:require [reagent.core :as reagent]
            [reagent.dom :refer [render]]
            [re-frame.core :as rf :include-macros true]
            [day8.re-frame.http-fx]
            [day8.re-frame.async-flow-fx]
            [stylefy.core :as stylefy]
            [clojure.string :as str]
            [cljs-karaoke.protocols :as protocols]
            [cljs-karaoke.songs :as songs]
            [cljs-karaoke.audio :as aud]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.mobile]
            [cljs-karaoke.key-bindings]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.modals :as modals]
            [cljs-karaoke.lyrics :as l :refer [ frame-text-string]]
            [cljs-karaoke.audio-input :refer [ spectro-overlay]]
            [cljs-karaoke.playback :as playback :refer [play stop]]
            [cljs-karaoke.views.dispatcher]
            [cljs-karaoke.views.billboards :refer [billboards-component]]
            [cljs-karaoke.views.page-loader :as page-loader]
            [cljs-karaoke.views.seek-buttons :as seek-buttons]
            [cljs-karaoke.views.control-panel :refer [control-panel]]
            [cljs-karaoke.views.lyrics :refer [frame-text]]
            [cljs-karaoke.views.playlist-mode :refer [playlist-view-component]]
            [cljs-karaoke.views.navbar :as navbar]
            [cljs-karaoke.editor.view  :refer [editor-component]]
            [cljs-karaoke.views.playback :refer [playback-controls lyrics-timing-progress song-progress seek song-time-display
                                                 show-song-metadata-modal]]
            [cljs-karaoke.views.toasty  :as toasty-views :refer [toasty trigger-toasty]]
            [cljs-karaoke.notifications :as notifications]
            [cljs-karaoke.router.core :as router]
            [cljs-karaoke.styles :as styles
             :refer [ centered screen-centered
                     top-left parent-style]]
            [shadow.loader :as loader]
            [cljs-karaoke.editor.core]

            [mount.core :as mount]
            [cljs.core.async :as async]))

(stylefy/init)


(def bg-style (rf/subscribe [::s/bg-style]))


(declare save-song fetch-all)
(defn save-current []
  (let [name (rf/subscribe [::s/current-song])
        lyrics (rf/subscribe [::s/lyrics])]
    (save-song @name @lyrics)))

(defn fetch-all-saved-songs []
  (.. (fetch-all)
      (then #(js->clj % :keywordize-keys true))
      (catch
          (fn [err]
            (println "error fetching" err)))))

(defn current-frame-display []
  (let [frame (rf/subscribe [::s/frame-to-display])]
    (when (and
           @(rf/subscribe [::s/song-playing?])
           (some? @frame)
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
   ^{:class "edge-stop-btn"}
   [playback-controls]
   [current-frame-display]
   (comment)
   #_[playback-debug-panel]
   [song-time-display (* 1000 @(rf/subscribe [::s/song-position]))]
   [billboards-component]
   (when (and
          @(rf/subscribe [::s/song-paused?])
          @(rf/subscribe [::s/can-play?]))
     [:div.screen-centered
      (when @(rf/subscribe
              [::s/view-property :playback :options-enabled?])
        [:a.top-left
          {:on-click #(rf/dispatch [::views-events/set-current-view :home])}
         [:span.icon
          [:i.fas.fa-cog.fa-3x]]])
      [:div.field.is-grouped
        (stylefy/use-style
         {:z-index 500})
        [:p.control>a.button.is-size-2.is-primary
         {:on-click play}
         [:span.icon>i.fas.fa-play.fa-fw
           (stylefy/use-style {:text-shadow "0px 0px 9px white"})]]
        (when @(rf/subscribe [::s/current-song-metadata])
          [:p.control>a.button.is-size-2.is-info
           {:on-click show-song-metadata-modal}
           [:span.icon>i.fas.fa-info.fa-fw
             (stylefy/use-style {:text-shadow "0px 0px 9px white"})]])]])
   (when-not @(rf/subscribe [::s/can-play?])
     [:a.centered
       {:on-click
        #(if-let [song @(rf/subscribe [::s/current-song])]
           (songs/load-song song)
           (songs/load-song))}
      [:span.icon
       [:i.fas.fa-sync.fa-5x]]])
   [seek-buttons/seek-component #(seek 10000) #(seek -10000)]
   (when-not @(rf/subscribe [::s/song-paused?])
     [:div.edge-progress-bar
      [lyrics-timing-progress]
      [song-progress]])])

(defn default-view []
  [:div.default-view.is-fluid
   [control-panel]
   [:button.button.is-danger.edge-stop-btn
    {:class (if @(rf/subscribe [::s/song-paused?])
              []
              ["song-playing"])
     :on-click stop}
    [:span.icon
     [:i.fas.fa-stop]]]
   (when-not @(rf/subscribe [::s/song-paused?])
     [:div.edge-progress-bar {:style {:position :relative}}
      [song-progress]])])



(defn pages [page-name]
  (case page-name
    :home     [default-view]
    :editor   [editor-component]
    :playlist [playlist-view-component]
    :playback [playback-view]
    [default-view]))


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
   (pages @(rf/subscribe [::s/current-view]))])

(defn ^:export load-song-global [s]
  (async/go-loop [_ (async/<! (async/timeout 1000))]
    (if  @(rf/subscribe [::s/initialized?])
      (do
        (println "app ready, loading song " s)
        (rf/dispatch-sync [::song-events/navigate-to-song s]))
      (do
        (println "Not yet ready, waiting ...")
        (recur (async/<! (async/timeout 1000)))))))

(defn get-sharing-url []
  (let [l        js/location
        protocol (. l -protocol)
        host     (. l -host)
        song     @(rf/subscribe [::s/current-song])]
    (->
     (str protocol "//" host "/songs/" song ".html")
     (js/encodeURI))))



(defonce app-root (. js/document (getElementById "root")))
 
(defn mount-components! []
  (render [app] app-root))
(defn init! []
  (println "init!")
  (router/app-routes)
  (rf/dispatch-sync [::events/init-db])
  (mount-components!)
  (mount/in-cljc-mode)
  (mount/start))


(defn ^:dev/after-load start-app []
  (println "start app, mounting components")
  (mount-components!))

(defn ^:dev/before-load stop-app []
  (println "stop app")
  (rf/clear-subscription-cache!))

(defn- capture-stream [^js/AudioBuffer audio]
  (cond
    (-> audio .-captureStream) (. audio (captureStream))
    (-> audio .-mozCaptureStream) (. audio (mozCaptureStream))
    :else  nil))

(defmethod aud/process-audio-event :canplaythrough
  [event]
  (println "handling canplaythrough event")
  (let [audio @(rf/subscribe [::s/audio])
        ;; output-mix @(rf/subscribe [::audio-subs/output-mix])
        ;; ctx @(rf/subscribe [::audio-subs/audio-context])
        song-stream (capture-stream audio)]
        ;; song-paused? @(rf/subscribe [::s/song-paused?])]
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

