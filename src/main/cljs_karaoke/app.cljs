(ns cljs-karaoke.app
  (:require [reagent.core :as reagent]
            [reagent.dom :refer [render]]
            [re-frame.core :as rf :include-macros true]
            [day8.re-frame.http-fx]
            [stylefy.core :as stylefy]
            [goog.labs.userAgent.device :as device]
            [clojure.string :as str]
            [cljs-karaoke.protocols :as protocols]
            [cljs-karaoke.songs :as songs]
            [cljs-karaoke.audio :as aud]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.events :as events]
            ;; [cljs-karaoke.mongo :as mongo]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.modals :as modals]
            [cljs-karaoke.lyrics :as l :refer [ frame-text-string]]
            [cljs-karaoke.audio-input :refer [ spectro-overlay]]
            [cljs-karaoke.playback :as playback :refer [play stop]]
            [cljs-karaoke.views.billboards :refer [billboards-component]]
            [cljs-karaoke.views.page-loader :as page-loader]
            [cljs-karaoke.views.seek-buttons :as seek-buttons]
            [cljs-karaoke.views.control-panel :refer [control-panel]]
            [cljs-karaoke.views.lyrics :refer [frame-text]]
            [cljs-karaoke.views.playlist-mode :refer [playlist-view-component]]
            [cljs-karaoke.views.navbar :as navbar]
            [cljs-karaoke.editor.view  :refer [editor-component]]
            [cljs-karaoke.views.playback :refer [playback-controls lyrics-timing-progress song-progress seek song-time-display]]
            [cljs-karaoke.views.toasty  :as toasty-views :refer [toasty trigger-toasty]]
            [cljs-karaoke.notifications :as notifications]
            [cljs-karaoke.router :as router]
            [cljs-karaoke.key-bindings :refer [init-keybindings!]]
            [cljs-karaoke.styles :as styles
             :refer [ centered screen-centered
                     top-left parent-style]]
            [shadow.loader :as loader]
            ["shake.js" :as Shake]))

(stylefy/init)

(defn- ios? []
  (-> (. js/navigator -platform)
      (str/lower-case)
      (str/includes? "ios")))

(defn- mobile-device? []
  (device/isMobile))

(defonce shake (Shake. (clj->js {:threshold 15 :timeout 1000})))

(.start shake)

(defonce my-shake-event (Shake. (clj->js {:threshold 15 :timeout 1000})))

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
   ^{:class "edge-stop-btn"} [playback-controls]
   [spectro-overlay]
   [current-frame-display]
   (comment)
   #_[playback-debug-panel]
   [song-time-display (* 1000 @(rf/subscribe [::s/song-position]))]
   [billboards-component]
   (when (and
          @(rf/subscribe [::s/song-paused?])
          @(rf/subscribe [::s/can-play?]))
     [:div (stylefy/use-style screen-centered)
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
     [:div.edge-progress-bar
      [song-progress]])])


(defn editor []
  (if-not (loader/loaded? "editor")
    (loader/load "editor" #(editor-component))
    ;; (require '[cljs-karaoke.editor.core])
    ;; ( '[cljs-karaoke.editor.view :refer [editor-component]]))
    [editor-component]))

(defn pages [page-name params]
  (case page-name
    :home     [default-view]
    :editor   [editor-component]
    ;; :songs            [playback-view]
    ;; :song             (do
    ;; (:cljs-karaoke.events.songs/load-song params)
    ;; [playback-view]]
    ;; :song-with-offset (do
    ;; (:cljs-karaoke.events.songs/load-song params)
    ;; [playback-view]]
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
   ;; [logo-animation]
   ;; [:div.page-content.roll-in-blurred-top
   (pages @(rf/subscribe [::s/current-view]) @(rf/subscribe [::s/current-song]))
   #_(when-let [_ (and)
                 @(rf/subscribe [::s/initialized?])
                 @(rf/subscribe [::s/current-view])]
       (condp = @(rf/subscribe [::s/current-view])
         :home     [default-view]
         :playback [playback-view]
         :playlist [playlist-view-component]
         :editor   [editor-component]))])

(defn ^:export load-song-global [s]
  (songs/load-song s))

#_(defn init-routing! []
  ;; (let [h (History.)]
    (secretary/set-config! :prefix "#")
    (defroute "/" []
      (println "home path")
      ;; (rf/dispatch-sync [::playlist-events/playlist-load])
      (rf/dispatch-sync [::views-events/view-action-transition :go-to-home]))
    (defroute "/songs/:song"
      [song query-params]
      (println "song: " song)
      (println "query params: " query-params)
      (rf/dispatch [::events/set-pageloader-active? true])
      ;; (rf/dispatch [::events/set-pageloader-exiting? false])
      (rf/dispatch [::views-events/set-current-view :playback])
      (if-some [offset (:offset query-params)]
        (rf/dispatch [::events/set-lyrics-delay (long offset)])
        ;; (do
        (rf/dispatch [::song-events/update-song-hash song]))
      (songs/load-song song)
      (when-some [_ (:show-opts query-params)]
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
    (gevents/listen ^js @(rf/subscribe [::s/history]) ^js EventType/NAVIGATE #(secretary/dispatch! (.-token ^js %)))
    (doto ^js @(rf/subscribe [::s/history]) (.setEnabled true)))


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

(defn on-shake [evt] (trigger-toasty))

(def mobile? (mobile-device?))

(defn init! []
  (println "init!")
  (rf/dispatch-sync [::events/init-db])
  (router/app-routes)
  (mount-components!)
  ;; (init-routing!)
  (if mobile?
    (do
      (println "mobile device, ignoring keybindings")
      (. js/window (addEventListener "shake" on-shake false))
      (when (ios?)
        (rf/dispatch-sync [::audio-events/set-audio-input-available? false])
        (rf/dispatch-sync [::audio-events/set-recording-enabled? false])))
    (init-keybindings!)))
  

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
    (rf/dispatch ^:flush-dom [::events/set-player-current-time (.-currentTime a)])))

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

