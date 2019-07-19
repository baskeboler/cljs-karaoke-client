(ns cljs-karaoke.app
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [cljs-karaoke.songs :as songs :refer [song-table-component]]
            [cljs-karaoke.audio :as aud :refer [setup-audio-listeners]]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.utils :as utils :refer [show-export-sync-info-modal]]
            [cljs-karaoke.lyrics :as l :refer [preprocess-frames frame-text-string]]
            [cljs.reader :as reader]
            [cljs.core.async :as async :refer [go go-loop chan <! >! timeout alts!]]
            [stylefy.core :as stylefy]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as gevents]
            [goog.history.EventType :as EventType]
            [keybind.core :as key]
            [clojure.string :as str]
            ["bulma-extensions"]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.views.page-loader :as page-loader]
            [cljs-karaoke.views.seek-buttons :as seek-buttons :refer [right-seek-component]])
  (:import goog.History))

(defonce s (stylefy/init))

(def wallpapers
  ["wp1.jpeg"
   "Dolphin.jpg"
   "wp2.jpg"
   "wp3.jpeg"
   "wp4.png"])

(def parent-style
  {:transition "background-image 1s ease-out"
   :background-size "cover"
   :background-image (str "url(\"images/" (first wallpapers) "\")")})

(def bg-style (rf/subscribe [::s/bg-style]))

(defn toggle-display-lyrics []
  (rf/dispatch [::events/toggle-display-lyrics]))

(defn toggle-display-lyrics-link []
  [:div.field>div.control
   [:a.button.is-info
    {:href "#"
     :on-click toggle-display-lyrics}
    (if @(rf/subscribe [::s/display-lyrics?])
      "hide lyrics"
      "show lyrics")]])

(defn return-after-timeout [obj delay]
  (let [ret-chan (chan)]
    ;; (if (pos? delay)
      (go
        (when  (>= delay 0)
          (<! (timeout delay)))
        (>! ret-chan obj))
    ret-chan))

(defn highlight-parts-2 [frame player-id]
  (let [part-chan (chan)
        current-status-id (rf/subscribe [::s/player-status-id])
        current-frame (rf/subscribe [::s/current-frame])
        part-tos (->> (:events frame)
                      (mapv (fn [evt]
                              (return-after-timeout evt (:offset evt)))))]
    (rf/dispatch-sync [::events/set-highlight-status part-tos])
    (go
      (doseq [_ (range (count part-tos))
              :let [[v ch] (async/alts! part-tos)]
              :while (and v (= player-id @current-status-id))]
        ;; (println "highlight-2" (:id v))
        (when (= player-id @current-status-id)
          (rf/dispatch-sync [::events/highlight-frame-part (:id frame) (:id v)]))))))

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

(defn play-lyrics-2
  ([frames offset]
   (let [frame-chan (chan 1000)
         part-chan (chan 10)
         status-id (random-uuid)
         remaining-frames (drop-while #(< (:offset %) offset) frames)
         current-frame (last (take-while #(< (:offset %) offset) frames))
         current-player-status-id (rf/subscribe [::s/player-status-id])
         song (rf/subscribe [::s/current-song])
         song-delay (rf/subscribe [::s/custom-song-delay @song])
         frames-tos (mapv (fn [fr]
                            (return-after-timeout
                             fr
                             (+ @song-delay
                                (- (:offset fr)
                                   offset))))
                          remaining-frames)]
     (rf/dispatch-sync [::song-events/set-player-status-id status-id])
     (rf/dispatch-sync [::events/set-current-frame current-frame])
     (go
       (doseq [_ (range (count remaining-frames))
               :let [[v ch] (async/alts! (conj frames-tos frame-chan) {:priority true})]
               :while (and
                       (not @(rf/subscribe [::s/song-paused?]))
                       (not (nil? v))
                       (not= frame-chan ch)              ;; (into frame-tos [frame-chan]))]]
                       (= @current-player-status-id status-id))]
           (case ch
             frame-chan (doseq [c frames-tos]
                          (async/close! c))
             (do
               (println "Dispatching frame")
               (rf/dispatch-sync [::events/set-current-frame v])
               (highlight-parts-2 v status-id))))
       (println "Finished lyrics play go-block"))
     frame-chan))
  ([frames] (play-lyrics-2 frames 0)))

(defn delay-select []
  (let [delay (rf/subscribe [::s/lyrics-delay])]
    [:div.field
     ;; [:label "Text delay (ms)"]
     [:div.control
      [:div.select.is-primary.is-fullwidth.delay-select
       [:select {:value @delay
                 :on-change #(rf/dispatch [::events/set-lyrics-delay (-> % .-target .-value (long))])}
        (for [v (vec (range -10000 10001 250))]
          [:option {:key (str "opt_" v)
                    :value v}
           v])]]]]))

(defn- clean-text [t]
  (-> t
      (str/replace #"/" "")
      (str/replace #"\\" "")))

(defn- leading? [t]
  (or (str/starts-with? t "/")
      (str/starts-with? t "\\")))

(defn leading-icon []
  [:span.icon (stylefy/use-style {:margin "0 0.5em"})
   [:i.fas.fa-music.fa-fw]])

(defn frame-text [frame]
  [:div.frame-text
   (for [e (vec (:events frame))
         :let [span-text (clean-text (:text e))]]
     [:span {:key (str "evt_" (:id e))
             :class (if (:highlighted? e) ["highlighted"] [])}
      (when (leading? (:text e)) [leading-icon]) span-text])])

(defn lyrics-view [lyrics]
  [:div.tile.is-child.is-vertical
   (for [frame (vec lyrics)]
     [:div [frame-text frame]])])

(defn current-frame-display []
  (let [frame (rf/subscribe [::s/current-frame])]
    (when (and
           @frame
           (not (str/blank? (frame-text-string @frame))))
      [:div.current-frame
       [frame-text @(rf/subscribe [::s/current-frame])]])))

(defn play []
  (let [audio (rf/subscribe [::s/audio])
        lyrics (rf/subscribe [::s/lyrics])]
    (rf/dispatch [::events/set-current-view :playback])
    ;; (rf/dispatch-sync [::events/set-player-status
                       ;; (play-lyrics-2 @lyrics)])
    (set! (.-currentTime @audio) 0)
    ;; (.play @audio)))
    (rf/dispatch [::events/play])))
(defn stop []
  (let [audio (rf/subscribe [::s/audio])
        current (rf/subscribe [::s/current-song])
        highlight-status (rf/subscribe [::s/highlight-status])
        player-status (rf/subscribe [::s/player-status])
        audio-events (rf/subscribe [::s/audio-events])
        stop-channel (rf/subscribe [::s/stop-channel])]
    (when @audio
      (.pause @audio)
      (set! (.-currentTime @audio) 0))
    (when @stop-channel
      (async/put!  @stop-channel :stop))
    (when @audio-events
      (async/close! @audio-events))
    (rf/dispatch-sync [::events/set-audio-events nil])
    (rf/dispatch-sync [::events/set-current-frame nil])
    (rf/dispatch-sync [::events/set-lyrics nil])
    (rf/dispatch-sync [::events/set-lyrics-loaded? false])
    (when-not (nil? @player-status)
      (async/close! @player-status))
    (when-not (nil? @highlight-status)
      (doseq [c @highlight-status]
        (async/close! c)))
    (rf/dispatch-sync [::events/set-playing? false])
    (rf/dispatch-sync [::events/set-highlight-status nil])
    (rf/dispatch-sync [::events/set-player-status nil])
    (songs/load-song @current)))

(defn select-current-frame [frames ms]
  (let [previous-frames (filter #(<= (:offset %) ms) frames)
        latest-offset (apply max (map :offset previous-frames))
        latest (first (filter #(= (:offset %) latest-offset) previous-frames))]
    latest))

(defn seek [offset]
  (let [player-status (rf/subscribe [::s/player-status])
        audio (rf/subscribe [::s/audio])
        highlight-status (rf/subscribe [::s/highlight-status])
        frames (rf/subscribe [::s/lyrics])
        pos (rf/subscribe [::s/player-current-time])]
    ;; (.pause @audio)
    (when-not (nil? @player-status)
      (async/close! @player-status))
    (doseq [c @highlight-status]
      (async/close! c))
    ;; (rf/dispatch-sync [::events/set-highlight-status nil])
    ;; (rf/dispatch-sync [::events/set-current-frame (select-current-frame @frames (+ (* 1000 @pos) offset))])
    (rf/dispatch-sync [::events/set-player-status
                         (play-lyrics-2 @frames (+ (* 1000 @pos) offset))])
    (set! (.-currentTime @audio) (+ @pos (/ (double offset) 1000.0)))))
    ;; (.play @audio)))

(defn toggle-song-list-btn []
  (let [visible? (rf/subscribe [::s/song-list-visible?])]
    [:button.button.is-fullwidth.tooltip
     {:class (concat []
                     (if @visible?
                       ["is-selected"
                        "is-success"]
                       ["is-danger"]))
      :data-tooltip "TOGGLE SONG LIST"
      :on-click #(rf/dispatch [::song-list-events/toggle-song-list-visible])}
     [:span.icon
      (if @visible?
        [:i.fas.fa-eye-slash];"Hide songs"
        [:i.fas.fa-eye])]])) ;"Show song list")]]))
(defn save-custom-delay-btn []
  (let [selected (rf/subscribe [::s/current-song])
        delay (rf/subscribe [::s/lyrics-delay])]
    [:button.button.is-primary
     {:disabled (nil? @selected)
      :on-click #(when-not (nil? @selected)
                   (rf/dispatch [::events/set-custom-song-delay @selected @delay]))}
     "remember song delay"]))

(defn export-sync-data-btn []
  [:button.button.is-info.tooltip
   {:on-click (fn [_]
                (utils/show-export-sync-info-modal))
    :data-tooltip "EXPORT SYNC INFO"}
   [:span.icon
    [:i.fas.fa-file-export]]])
    ;; "export sync data"]])

(defn info-table []
  (let [current-song (rf/subscribe [::s/current-song])
        lyrics-loaded? (rf/subscribe [::s/lyrics-loaded?])]
    [:table.table.is-fullwidth
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

(defn control-panel []
  (let [lyrics (rf/subscribe [::s/lyrics])
        display-lyrics? (rf/subscribe [::s/display-lyrics?])
        current-song (rf/subscribe [::s/current-song])
        lyrics-loaded? (rf/subscribe [::s/lyrics-loaded?])
        song-list-visible? (rf/subscribe [::s/song-list-visible?])
        can-play? (rf/subscribe [::s/can-play?])]
    [:div.control-panel.columns
     {:class (if @(rf/subscribe [::s/song-paused?])
               ["song-paused"]
               ["song-playing"])}
     [:div.column (stylefy/use-style {:background-color "rgba(1,1,1, .3)"})
      [toggle-display-lyrics-link]
      [delay-select]
      [info-table]
      [:div.columns>div.column.is-12
       [:div.field.has-addons
        [:div.control
         [:button.button.is-primary {:on-click #(songs/load-song @current-song)}
          [:span.icon
           [:i.fas.fa-folder-open]]]]
        [:div.control
         [:button.button.is-info.tooltip
          (if @can-play?
            {:on-click play
             :data-tooltip "PLAY"}
            {:disabled true})
          [:span.icon
           [:i.fas.fa-play]]]]
        [:div.control
         [:button.button.is-warning.stop-btn.tooltip
          {:on-click stop
           :data-tooltip "STOP"}
          [:span.icon
           [:i.fas.fa-stop]]]]
        [:div.control
         [export-sync-data-btn]]
        [:div.control
         [toggle-song-list-btn]]]
       [:div.field
        [:div.control
         [save-custom-delay-btn]]]]]
     (when @display-lyrics?
       [:div.column (stylefy/use-style {:background-color "rgba(1,1,1, .3)"})
        [lyrics-view @lyrics]])
     (when @song-list-visible?
       [:div.column
        [song-table-component]])]))

(def centered {:position :fixed
               :display :block
               :top "50%"
               :left "50%"
               :transform "translate(-50%, -50%)"})
(def top-left {:position :fixed
               :display :block
               :top 0
               :left 0
               :margin "2em 2em"})

(def time-display-style
  {:position :fixed
   :display :block
   :color :white
   :font-weight :bold
   :top 0
   :left "50%"
   :transform "translate(-50%)"
   :margin "1em"
   :border-radius ".5em"
   :padding "0.5em"
   :background-color "rgba(0,0,0, 0.3)"})

(defn song-time-display [^double ms]
  (let [secs (-> ms
                 (/ 1000.0)
                 (mod 60.0)
                 long)
        mins (-> ms
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
     (stylefy/use-style time-display-style)
     [:span.hours hours] ":"
     [:span.minutes mins] ":"
     [:span.seconds secs] "."
     [:span.milis (-> ms (mod 1000) long)]]))

(def view-states
  {:home {:go-to-playback :playback
          :go-to-home :home
          :play :home}
   :playback {:play :playback
              :stop :playback
              :go-to-home :home
              :go-to-playback :playback}
   :admin {}})

(def transition  (partial  view-states))

(defn icon-button [icon button-type callback]
  [:div.control
   [:p.control
    [:a.button
     {:class ["button" (str "is-" button-type)]
      :on-click callback}
     [:span.icon.is-small
      [:i
       {:class ["fa" (str "fa-" icon)]}]]]]])
(def top-right
  {:position :absolute
   :top "0.5em"
   :right "0.5em"})

(defn playback-controls []
  [:div.playback-controls.field.has-addons
   (stylefy/use-style top-right)
   (when-not @(rf/subscribe [::s/song-paused?])
     [icon-button "stop" "danger" stop])
   [icon-button "forward" "info" #(do
                                    (stop)
                                    (rf/dispatch [::playlist-events/playlist-next]))]])

(defn playback-view []
  [:div.playback-view
   [current-frame-display]
   [song-time-display (* 1000 @(rf/subscribe [::s/song-position]))]
   (when (and
          @(rf/subscribe [::s/song-paused?])
          @(rf/subscribe [::s/can-play?]))
     [:div
      (when @(rf/subscribe
              [::s/view-property :playback :options-enabled?])
        [:a
         (stylefy/use-style
          top-left
          {:on-click #(rf/dispatch [::events/set-current-view :home])})
         [:span.icon
          [:i.fas.fa-cog.fa-3x]]])
      [:a
       (stylefy/use-style
        centered
        {:on-click play})
       [:span.icon
        [:i.fas.fa-play.fa-5x]]]])
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
   #_[:button.button.is-danger.edge-stop-btn
      {:class (if @(rf/subscribe [::s/song-paused?])
                []
                ["song-playing"])
       :on-click stop}
      [:span.icon
       [:i.fas.fa-stop]]]
   [seek-buttons/seek-component #(seek 10000) #(seek -10000)]
   (when-not @(rf/subscribe [::s/song-paused?])
     [:div.edge-progress-bar
      [song-progress]])])

(defn default-view []
  [:div.default-view
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
              {:position :fixed
               :bottom "-474px"
               :left "0"
               :opacity 0
               :z-index 2
               :display :block
               :transition "all 0.5s ease-in-out"}
              (if @toasty {:bottom "0px"
                           :opacity 1})))
       [:img {:src "images/toasty.png" :alt "toasty"}]])))

(defn trigger-toasty []
  (let [a (js/Audio. "media/toasty.mp3")]
    (.play a)
    (rf/dispatch [::events/trigger-toasty])))

(defn app []
  [:div.container.app
   [toasty]
   [utils/modals-component]
   [page-loader/page-loader-component]
   [:div.app-bg (stylefy/use-style (merge parent-style @bg-style))]

   (when-let [_ @(rf/subscribe [::s/initialized?])]
     (condp = @(rf/subscribe [::s/current-view])
       :home [default-view]
       :playback [playback-view]))])

(defn init-routing! []
  (secretary/set-config! :prefix "#")
  (defroute "/" []
    (println "home path")
    (rf/dispatch-sync [::playlist-events/playlist-load]))
    ;; (songs/load-song @(rf/subscribe [::r/playlist-current])))
  (defroute "/songs/:song"
    [song query-params]
    (println "song: " song)
    (println "query params: " query-params)
    (songs/load-song song)
    (when-some [offset (:offset query-params)]
      (rf/dispatch [::events/set-lyrics-delay (long offset)])
      (rf/dispatch [::events/set-custom-song-delay song (long offset)]))
    (when-some [show-opts? (:show-opts query-params)]
      (rf/dispatch [::views-events/set-view-property :playback :options-enabled? true])))

  ;; Quick and dirty history configuration.
  (defroute "/party-mode" []
    (println "fuck yea! party mode ON")
    (rf/dispatch [::playlist-events/set-loop? true])
    (rf/dispatch [::playlist-events/playlist-load]))

  (let [h (History.)]
    (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
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
               (when-let [_ @(rf/subscribe [::s/loop?])]
                 (rf/dispatch-sync [::events/set-loop? false]))
               (when-not (nil? @(rf/subscribe [::s/player-status]))
                 (stop))))
  (key/bind! "l r" ::l-r-kb #(songs/load-song))
  (key/bind! "alt-o" ::alt-o #(rf/dispatch [::views-events/set-view-property :playback :options-enabled? true]))
  (key/bind! "alt-h" ::alt-h #(rf/dispatch [::events/set-current-view :home]))
  (key/bind! "left" ::left #(seek -10000.0))
  (key/bind! "right" ::right #(seek 10000.0))
  (key/bind! "meta-shift-l" ::loop-mode #(do
                                           (rf/dispatch [::events/set-loop? true])
                                           (rf/dispatch [::playlist-events/playlist-load])))
  (key/bind! "alt-shift-p" ::alt-meta-play #(play))
  (key/bind! "shift-right" ::shift-right #(do
                                            (stop)
                                            (rf/dispatch [::playlist-events/playlist-next])))
  (key/bind! "t t" ::double-t #(trigger-toasty)))
(defn mount-components! []
  (reagent/render
   [app]
   (. js/document (getElementById "root"))))

(defn init! []
  (println "init!")
  ;; (rf/dispatch [::events/fetch-custom-delays])
  ;; (rf/dispatch [::events/fetch-song-background-config])
  ;; (rf/dispatch [::song-list-events/init-song-list-state])
  ;; (rf/dispatch [::views-events/init-views-state])
  ;; (rf/dispatch-sync [::events/build-verified-playlist])
  ;; (rf/dispatch [::events/init-song-bg-cache])
  (mount-components!)
  (rf/dispatch [::events/init-db])
  (init-keybindings!)
  (init-routing!))
  ;; (async/go
  ;;   (loop [ready (rf/subscribe [::s/initialized?])]
  ;;     (when (or (undefined? ready)
  ;;               (not @ready))
  ;;       (println "waiting for init")
  ;;       (async/<!
  ;;        (async/timeout 100))
  ;;       (recur (rf/subscribe [::s/initialized?]))))))


(defmethod aud/process-audio-event :canplaythrough
  [event]
  (println "handling canplaythrough event")
  (rf/dispatch-sync [::events/set-can-play? true])
  (let [audio @(rf/subscribe [::s/audio])
        song-paused? @(rf/subscribe [::s/song-paused?])])
    ;; (when song-paused?
      ;; (.play audio)
      ;; (.pause audio)
      ;; (rf/dispatch-sync [::events/set-player-current-time 0]))
  #_(when-let [_ (and)
               @(rf/subscribe [::s/loop?])
               @(rf/subscribe [::s/song-paused?])]
      (play)))
(defn ->ms [secs]
  (* 1000 secs))
(defn ->secs [ms]
  (/ (double ms) 1000.0))

(defn update-karaoke-player-status []
  (let [current-time (rf/subscribe [::s/player-current-time])
        lyrics (rf/subscribe [::s/lyrics])
        new-status (play-lyrics-2 @lyrics (->ms @current-time))]
    (rf/dispatch-sync [::events/set-player-status new-status])))

(defmethod aud/process-audio-event :timeupdate
  [event]
  (when-let [a @(rf/subscribe [::s/audio])]
    (rf/dispatch-sync [::events/set-player-current-time (.-currentTime a)])))
(defmethod aud/process-audio-event :play
  [event]
  (println "play event")
  ;; (rf/dispatch-sync [::events/set-player-current-time 0])
  (update-karaoke-player-status))
  ;; (rf/dispatch-sync [::events/set-player-status (play-lyrics-2 @(rf/subscribe [::s/lyrics]))]))
  
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
;; (songs/load-song)))
    (rf/dispatch [::playlist-events/playlist-next])))
