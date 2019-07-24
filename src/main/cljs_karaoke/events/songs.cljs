(ns cljs-karaoke.events.songs
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.async-flow-fx]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.common :as common-events]
            [cljs-karaoke.events.backgrounds :as bg-events]
            [cljs-karaoke.events.lyrics :as lyrics-events]
            [cljs-karaoke.audio :as aud]
            [cljs-karaoke.lyrics :refer [preprocess-frames]]
            [cljs.core.async :as async :refer [go go-loop <! >! chan]]))

                                        ; fetch song delay, fetch song background
(defn load-song-flow []
  {
   ;; :first-dispatch [::load-song-start song-name]

     :rules [{:when :seen-all-of?
              :events [
                       ::lyrics-events/fetch-lyrics-complete
                       ::bg-events/update-bg-image-flow-complete
                       ::setup-audio-complete]
              :dispatch-n [[::events/set-pageloader-active? false]
                           [::events/set-can-play? true]]
              :halt? true}]})

(defn stop-song-flow []
  {:first-dispatch [::stop-song-start]
   :rules [:when :seen-all-of?
           :events [::audio-stopped ::audio-events-closed]
           :dispatch-n [
                        ;; [::events/set-audio-events nil]
                        [::events/set-current-frame nil]
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

;; (rf/reg-event-fx
;;  ::trigger-load-song-flow
;;  (fn-traced
;;   [{:keys [db]} [_ song-name]]
;;   {:db db
;;    :async-flow (load-song-flow song-name)}))

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
  {:db db
   :async-flow (load-song-flow)
   :dispatch-n [[::events/set-pageloader-active? true]
                [::events/set-can-play? false]
                [::events/set-playing? false]
                [::setup-audio-events song-name]
                [::update-song-hash song-name]
                [::common-events/set-page-title (str "Karaoke :: " song-name)]
                [::events/set-current-song song-name]
                [::bg-events/init-update-bg-image-flow song-name]
                [::lyrics-events/fetch-lyrics song-name preprocess-frames]
                [::events/set-current-view :playback]]})) 

(rf/reg-event-fx
 ::setup-audio-events
 (rf/after
  (fn [db [_ song-name]]
    (. js/console (log "setup audio: " song-name  ", storage: " (get db :base-storage-url "")))
    (let [base-storage-url (get db :base-storage-url "")
          audio-path (str base-storage-url "/mp3/" song-name ".mp3")
          audio (.  js/document (getElementById "main-audio"))]
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

(rf/reg-event-db
 ::set-player-status-id
 (fn-traced [db [_ id]]
    (-> db (assoc :player-status-id id))))
