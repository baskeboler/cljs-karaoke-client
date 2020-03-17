(ns cljs-karaoke.events
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [ajax.core :as ajax]
            [cljs.core.async :as async :refer [go go-loop <! >! chan]]
            [day8.re-frame.async-flow-fx]
            [cljs.reader :as reader]
            [clojure.string :refer [replace]]
            [cljs-karaoke.lyrics :refer [preprocess-frames]]
            [cljs-karaoke.search :as search]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.events.common :as common-events
             :refer [reg-set-attr
                     save-to-localstorage get-from-localstorage
                     get-custom-delays-from-localstorage set-location-hash]]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.notifications]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.events.user :as user-events]
            [cljs-karaoke.events.metrics :as metrics-events]
            [cljs-karaoke.events.editor :as editor-events]
            [cljs-karaoke.config :refer [config-map]]
            ;; [cljs-karaoke.events.http-relay :as http-relay-events]
            [goog.events :as gevents]
            [cljs-karaoke.audio :as aud])
  (:import goog.History))
(defonce fetch-bg-from-web-enabled? true)
(def base-storage-url (:audio-files-prefix config-map))


(declare save-custom-delays-to-localstore)


(defn init-flow []
  {
   ;; :id    (gensym ::init-flow)
   :rules [{:when     :seen?
            :events   [::handle-fetch-background-config-complete]
            :dispatch [::init-song-bg-cache]}
           {:when     :seen?
            :events   [::handle-fetch-delays-complete]
            :dispatch [::playlist-events/build-verified-playlist]}
           {:when       :seen-any-of?
            :events     [::handle-fetch-background-config-failure
                         ::handle-fetch-delays-failure]
            :dispatch-n [[::pageloader-exit-transition]
                         ;; [::set-pageloader-active? false]
                         [::boot-failure]]
            :halt?      true}
           {:when       :seen?
            :events     ::playlist-ready
            :dispatch-n [[::views-events/set-current-view :playback]
                         [::playlist-load]]}
           {:when       :seen-all-of?
            :events     [::song-bgs-loaded
                         ::song-delays-loaded
                         ::set-audio
                         ;; ::http-relay-events/init-http-relay-listener
                         ::set-audio-events
                         ::metrics-events/load-metrics-from-localstorage-complete
                         ::initial-audio-setup-complete
                         ::playlist-events/playlist-ready
                         ::views-events/views-state-ready
                         ::song-list-events/song-list-ready
                         ::fetch-song-list-complete]
            :dispatch-n [[::pageloader-exit-transition]
                         ;; [::set-pageloader-active? false]
                         [::initialized]]
            :halt?      true}]})
(rf/reg-event-db
 ::boot-failure
 (fn [db [_ e]]
   (.log js/console "Failed to boot: " e)
   (-> db
       (assoc :boot-failed? true))))

(rf/reg-event-db
 ::initialized
 (fn-traced
  [db _]
  (. js/console (log "initialized!"))
  (-> db
      (assoc :initialized? true))))

(rf/reg-event-fx
 ::init-db
 ;; ::init-fetches
 (fn-traced [_ _]
            {:db         {:current-frame              nil
                          :app-name                   "Karaoke Party"
                          :lyrics                     nil
                          :lyrics-loaded?             false
                          :lyrics-fetching?           false
                          :lyrics-delay               -1000
                          ;; :audio nil
                          :remote-control-id          ""
                          :audio-events               nil
                          :display-lyrics?            false
                          :current-song               nil
                          ;; :player-status nil
                          :can-play?                  false
                          ;; :highlight-status nil
                          :playing?                   false
                          :toasty?                    false
                          :player-current-time        0
                          :song-duration              0
                          :custom-song-delay          {}
                          :song-backgrounds           {}
                          :metrics                    {}
                          :stop-channel               (chan)
                          :loop?                      true
                          :initialized?               false
                          :base-storage-url           "https://karaoke-files.uyuyuy.xyz"
                          :current-view               :home
                          :pageloader-active?         true
                          :pageloader-exiting?        false
                          :display-home-button?       true
                          ;; :playlist (pl/build-playlist)
                          :navbar-menu-active?        false
                          :fetch-bg-from-web-enabled? true
                          :user                       nil
                          :billboards                 []
                          :modals                     []
                          :history                    (History.)
                          :notifications              []}
             ;; :dispatch-n [[::fetch-custom-delays]
             ;; [::fetch-song-background-config]}
             ;; [::init-song-bg-cache]]}))
             :async-flow (init-flow)

             :dispatch-n [[::fetch-custom-delays]
                          [::metrics-events/load-user-metrics-from-localstorage]
                          [::fetch-song-list]
                          [::editor-events/init]
                          [::fetch-song-background-config]
                          [::initial-audio-setup]
                          [::audio-events/init-audio-data]
                          [::views-events/init-views-state]
                          ;; [::http-relay-events/init-http-relay-listener]
                          [::song-list-events/init-song-list-state]
                          [::user-events/init]]}))
;; (rf/reg-event-fx
;;  ::init-fetches
;;  (fn-traced
;;   [{:keys [db]} _]
;;   {:db db
;;    :dispatch-n [[::fetch-custom-delays]
;;                 [::fetch-song-background-config]
;;                 [::views-events/init-views-state]
;;                 [::initial-audio-setup]
;;                 [::song-list-events/init-song-list-state]]}))

(rf/reg-event-db
 ::toggle-toasty?
 (fn-traced
  [db _]
  (-> db (update :toasty? not))))

(rf/reg-event-fx
 ::trigger-toasty
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::toggle-toasty?]
   :dispatch-later [{:ms 800 :dispatch [::toggle-toasty?]}]}))

(rf/reg-event-fx
 ::http-fetch-fail
 (fn-traced
  [{:keys [db]} [_ err dispatch-n-vec]]
  (println "fetch failed" err)
  {:db db
   :dispatch-n dispatch-n-vec}))

(rf/reg-event-fx
 ::fetch-song-list
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio {:method :get
                :uri "/data/songs.edn"
                :timeout 8000
                :response-format (ajax/text-response-format)
                :on-success [::handle-fetch-song-list-success]
                :on-failure [::http-fetch-fail [[::fetch-song-list-complete]]]}}))

(rf/reg-event-fx
 ::handle-fetch-song-list-success
 (fn-traced
  [{:keys [db]} [_ response]]
  {:db (-> db
           (assoc :available-songs (reader/read-string response)))
   :dispatch [::fetch-song-list-complete]}))

(rf/reg-event-db
 ::fetch-song-list-complete
 (fn-traced
  [db _]
  (println "fetch song list complete!")
  db))

(rf/reg-event-fx
 ::pageloader-exit-transition
 (fn-traced
  [{:keys [db] } _]
  {:db db
   :dispatch [::set-pageloader-exiting? true]
   :dispatch-later [{:ms 3000
                     :dispatch [::set-pageloader-active? false]}
                    {:ms 3000
                     :dispatch [::set-pageloader-exiting? false]}]}))
(rf/reg-event-fx
 ::fetch-custom-delays
 (fn-traced
  [{:keys [db]} _]
  {:db         db
   :http-xhrio {:method          :get
                :uri             "/data/delays.edn"
                :timeout         8000
                :response-format (ajax/text-response-format)
                :on-success      [::handle-fetch-delays-success]
                :on-failure      [::handle-fetch-delays-failure]}}))
(rf/reg-event-fx
 ::handle-fetch-delays-success
 (fn-traced
  [{:keys [db]} [_ delays-resp]]
  (let [r (-> delays-resp
              (reader/read-string))]
    {:db db
     :dispatch [::merge-remote-delays-with-local r]})))
(rf/reg-event-fx
 ::merge-remote-delays-with-local
 (fn-traced
  [{:keys [db]} [_ remote-delays]]
  (let [local-delays (common-events/get-custom-delays-from-localstorage)
        delays       (merge
                      (if-not (nil? local-delays)
                        local-delays
                        {})
                      remote-delays)]
    {:db       (-> db
                   (assoc :custom-song-delay (merge delays)))
     :dispatch [::handle-fetch-delays-complete]})))

(rf/reg-event-fx
 ::save-custom-song-delays-to-localstorage
 (fn-traced
  [{:keys [db]} _]
  {:db       db
   :dispatch [::common-events/save-to-localstorage
              "custom-song-delays"
              (:custom-song-delay db)
              ::save-custom-delays-to-localstorage-complete]}))


(common-events/reg-identity-event ::save-custom-delays-to-localstorage-complete)

;; (rf/reg-event-db
 ;; ::save-custom-song-delays-to-localstorage-complete
 ;; (fn-traced [db _] db))

(rf/reg-event-fx
 ::handle-fetch-delays-complete
 (fn [{:keys [db]} _]
   {:db db
    :dispatch-n [[::song-delays-loaded]
                 [::common-events/save-to-localstorage
                  "custom-song-delays"
                  (:custom-song-delay db {})]]}))
(rf/reg-event-fx
 ::handle-fetch-delays-failure
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::song-delays-loaded]}))
(rf/reg-event-fx
 ::fetch-song-background-config
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio {:method :get
                :uri (str "/data/backgrounds.edn")
                :timeout 8000
                :response-format (ajax/text-response-format)
                :on-success [::handle-fetch-background-config-success]
                :on-failure [::handle-fetch-background-config-failure]}}))
(rf/reg-event-fx
 ::handle-fetch-background-config-success
 (fn-traced
  [{:keys [db] :as cofx} [_ a]]
  (let [c (-> a
              (reader/read-string))]
    {:db (-> db
             (update :song-backgrounds merge c))
     :dispatch [::handle-fetch-background-config-complete]})))

(rf/reg-event-fx
 ::handle-fetch-background-config-failure
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::handle-fetch-background-config-complete]}))

(rf/reg-event-db
 ::handle-fetch-background-config-complete
 (fn-traced [db _] db))

(rf/reg-event-db
 ::set-location-hash
 (rf/after
  (fn [db [_ new-hash]]
    (println "setting new location hash")
    (doto ^js (:history db) (.setEnabled false))
    (set-location-hash new-hash)
    (doto ^js (:history db) (.setEnabled true))))
 (fn-traced
  [db [_ new-hash]]
  (-> db
      (assoc :location-hash new-hash))))

(rf/reg-event-fx
 ::init-song-delays
 (fn-traced
  [{:keys [db]} _]
  (let [delays (get-custom-delays-from-localstorage)]
    {:db       (if-not (nil? delays)
                 (-> db
                     (update :custom-song-delay merge delays {}))
                 db)
     :dispatch [::song-delays-loaded]})))

(rf/reg-event-db
 ::song-delays-loaded
 (fn-traced
  [db _]
  (-> db
      (assoc :song-delays-loaded true))))

(rf/reg-event-fx
 ::init-song-bg-cache
 (fn-traced
  [{:keys [db]} _]
  (let [cache (get-from-localstorage "song-bg-cache")]
    {:db       (if-not (nil? cache)
                 (-> db
                     (update :song-backgrounds
                             merge cache)
                     (assoc :song-backgrounds-loaded? true))
                 db)
     :dispatch [::song-bgs-loaded]})))

(rf/reg-event-db
 ::song-bgs-loaded
 (fn-traced
  [db _]
  (. js/console (log "song backgrounds loaded"))
  db))

(reg-set-attr ::set-initialized? :initialized?)
(reg-set-attr ::set-loop? :loop?)
(reg-set-attr ::set-audio-events :audio-events)
(reg-set-attr ::set-song-duration :song-duration)
;; (reg-set-attr ::set-current-frame :current-frame)
(reg-set-attr ::set-audio :audio)
(reg-set-attr ::set-lyrics :lyrics)
(reg-set-attr ::set-lyrics-delay :lyrics-delay)
(reg-set-attr ::set-lyrics-loaded? :lyrics-loaded?)
(reg-set-attr ::set-display-lyrics? :display-lyrics?)
(reg-set-attr ::set-can-play? :can-play?)
(reg-set-attr ::set-player-current-time :player-current-time)
(reg-set-attr ::set-playing? :playing?)
(reg-set-attr ::set-pageloader-active? :pageloader-active?)
(reg-set-attr ::set-pageloader-exiting? :pageloader-exiting?)
(reg-set-attr ::set-navbar-menu-active? :navbar-menu-active?)

(rf/reg-event-db
 ::toggle-display-lyrics
 (fn-traced
  [db _]
  (-> db
      (update :display-lyrics? not))))

(rf/reg-event-fx
 ::set-current-song
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db         (-> db
                   (assoc :current-song song-name))
   :dispatch-n []}))
                ;; [::fetch-bg song-name]
                ;; [::set-lyrics-delay (get-in db [:custom-song-delay song-name] (get db :lyrics-delay))]]}))

;; (reg-set-attr ::set-player-status :player-status)
;; (reg-set-attr ::set-highlight-status :highlight-status)

(rf/reg-event-fx
 ::play
 (fn-traced
  [{:keys [db]} _]
  {:db       db
   :dispatch [::set-playing? true]}))



(rf/reg-event-fx
 ::set-custom-song-delay
 (fn-traced
  [{:keys [db]} [_ song-name delay]]
  {:db (-> db
           (update :custom-song-delay merge {song-name delay}))
   :dispatch-n [
                [::playlist-events/add-song song-name]
                [::save-custom-song-delays-to-localstorage]]}))



(rf/reg-event-fx
 ::initial-audio-setup
 (rf/after
  (fn [db _]
    (. js/console (log "Initial Audio Setup"))
    (let [audio        (. js/document (getElementById "main-audio"))
          audio-events (aud/setup-audio-listeners audio)]
      (go-loop [e (<! audio-events)]
        (when-not (nil? e)
          (aud/process-audio-event e)
          (recur (<! audio-events))))
      (rf/dispatch [::set-audio audio])
      (rf/dispatch [::set-audio-events audio-events]))))
 (fn-traced
  [{:keys [db]} _]
  {:db       db
   :dispatch [::initial-audio-setup-complete]}))

(rf/reg-event-db
 ::initial-audio-setup-complete
 (fn-traced
  [db _]
  (. js/console (log "Initial Audio Setup Complete!"))
  (-> db (assoc :initial-audio-setup-complete? true))))

