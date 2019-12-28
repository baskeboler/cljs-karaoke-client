(ns cljs-karaoke.events.songs
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.async-flow-fx]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.billboards :as billboard-events]
            [cljs-karaoke.events.common :as common-events]
            [cljs-karaoke.events.backgrounds :as bg-events]
            [cljs-karaoke.events.lyrics :as lyrics-events]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.metrics :as metrics-events]
            [cljs-karaoke.audio :as aud]
            [cljs-karaoke.lyrics :refer [preprocess-frames]]
            [cljs.core.async :as async :refer [go go-loop <! >! chan]]
            [cljs-karaoke.notifications :as n]
            [cljs-karaoke.events.notifications :as notification-events]))
(defn load-song-flow [song-name]
  {;; :first-dispatch [::load-song-start song-name]
   :rules [;; {:when     :seen?
           ;;  :events   [::metrics-events/load-metrics-from-localstorage-complete]
           ;;  :dispatch [::metrics-events/inc-song-play-count song-name]}
           {:when       :seen-all-of?
            :events     [::lyrics-events/fetch-lyrics-complete
                         ::bg-events/update-bg-image-flow-complete
                         ::setup-audio-complete
                         ::metrics-events/save-user-metrics-to-localstorage-complete]
            :dispatch-n [[::events/set-pageloader-active? false]
                         [::events/set-can-play? true]
                         [::billboard-events/display-billboard {:id       (random-uuid)
                                                                :type     :song-name-display
                                                                :text     song-name
                                                                :visible? true}
                          5000]]
            :halt?      true}]})

(defn stop-song-flow []
  {:first-dispatch [::stop-song-start]
   :rules          [:when :seen-all-of?
                    :events [::audio-stopped ::audio-events-closed]
                    :dispatch-n [;; [::events/set-audio-events nil]
                                 ;; [::events/set-current-frame nil]
                                 [::events/set-lyrics nil]
                                 [::events/set-lyrics-loaded?false]]]})

(rf/reg-event-fx
 ::stop-song-start
 (fn-traced
  [{:keys [db]} _]
  (when-let [a (get db :audio)]
    (.pause a))
  (rf/dispatch [::audio-stopped])
  (when-let [e (get db :audio-events)]
    (async/close! e))
  (rf/dispatch [::audio-events-closed])
  {:db db}))

(rf/reg-event-fx
 ::update-song-hash
 (fn-traced
  [{:keys [db]} [_ song-name]]
  (let [offset (get-in db [:custom-song-delay song-name] (get db :lyrics-delay 0))]
    {:db db
     :dispatch [::events/set-location-hash (str "/songs/" song-name "?offset=" offset)]})))

(rf/reg-event-fx
 ::trigger-load-song-flow

 ;; ::load-song-start
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db         db
   :async-flow (load-song-flow song-name)
   :dispatch-n [
                ;; [::events/set-pageloader-active? true]
                [::events/set-can-play? false]
                [::events/set-playing? false]
                [::metrics-events/load-user-metrics-from-localstorage]
                [::setup-audio-events song-name]
                ;; [::update-song-hash song-name]
                ;; [::set-first-playback-position-updated? false]
                ;; [::common-events/set-page-title (str "Karaoke :: " song-name)]
                [::events/set-current-song song-name]
                [::bg-events/init-update-bg-image-flow song-name]
                [::lyrics-events/fetch-lyrics song-name preprocess-frames]
                [::views-events/view-action-transition :load-song]]}))

(rf/reg-event-fx
 ::trigger-load-random-song
 (fn-traced
  [{:keys [db]} _]
  (let [song-name (->> db :available-songs rand-nth)]
    {:db       db
     :dispatch [::navigate-to-song song-name]})))

(rf/reg-event-fx
 ::setup-audio-events
 (rf/after
  (fn [db [_ song-name]]
    (. js/console (log "setup audio: " song-name  ", storage: " (get db :base-storage-url "")))
    (let [base-storage-url (get db :base-storage-url "")
          audio-path       (str events/base-storage-url "/mp3/" song-name ".mp3")
          audio            (.  js/document (getElementById "main-audio"))]
      (set! (.-src audio) audio-path)
      (rf/dispatch [::events/set-player-current-time 0])
      (rf/dispatch [::setup-audio-complete]))))
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db db}))

(rf/reg-event-db
 ::setup-audio-complete
 (fn-traced
  [db _]
  (. js/console (log "setup audio complete!"))
  db))

(cljs-karaoke.events.common/reg-set-attr ::set-song-stream :song-stream)

(defn save-delays-flow []
  {:rules [{:when     :seen?
            :events   [::events/set-custom-song-delay]
            :dispatch [::events/save-custom-song-delays-to-localstorage]
            :halt?    true}]})

(defn forget-delay-flow []
  {:rules [{:when     :seen?
            :events   [::forget-custom-song-delay]
            :dispatch-n [[::events/save-custom-song-delays-to-localstorage]
                         [:cljs-karaoke.events.playlists/build-verified-playlist]]
            :halt?    true}]})

(rf/reg-event-fx
 ::inc-current-song-delay
 (fn-traced
  [{:keys [db]} [_ delta]]
  {:db (-> db
           (update-in [:lyrics-delay] (partial + delta)))
   :async-flow (save-delays-flow)
   :dispatch-n [[::events/set-custom-song-delay (:current-song db) (+ (:lyrics-delay db) delta)]
                [::notification-events/add-notification (n/notification (str "sync'ed lyrics by " (+ (:lyrics-delay db) delta) " ms"))]]}))

(rf/reg-event-fx
 ::forget-custom-song-delay
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db (-> db
           (update :custom-song-delay dissoc song-name))
   :async-flow (forget-delay-flow)}))

(rf/reg-event-fx
 ::navigate-to-song
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db db
   :dispatch [::update-song-hash song-name]}))

(rf/reg-event-fx
 ::set-audio-playback-rate
 (rf/after
  (fn [db [_ rate]]
    (set! (. (-> db :audio) -playbackRate) rate)))
 (fn-traced
  [{:keys [db]} [_ rate]]
  {:db db}))
