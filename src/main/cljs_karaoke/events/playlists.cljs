(ns cljs-karaoke.events.playlists
  (:require [re-frame.core :as rf :include-macros true]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.protocols :as protocols]
            ;; [cljs-karaoke.events :as events]
            ;; [cljs-karaoke.events.songs :as song-events]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-event-fx
 ::playlist-load
 (fn-traced
  [{:keys [db]} _]
  {:db db
   ;; :dispatch-later [{:ms 2000
                     ;; :dispatch [::set-current-playlist-song]}))
   :dispatch [::build-verified-playlist]}))
(rf/reg-event-fx
 ::set-current-playlist-song
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch (if-not (nil? (:playlist db))
               [:cljs-karaoke.events.songs/navigate-to-song  (protocols/current (:playlist db))]
               [::playlist-load])}))

(rf/reg-event-fx
 ::jump-to-playlist-position
 (fn-traced
  [{:keys [db]} [_ position]]
  {:db (-> db
           (update :playlist assoc :current position))
   :dispatch [::set-current-playlist-song]}))

(rf/reg-event-fx
 ::add-song
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db (if-not (protocols/contains-song? (:playlist db) song-name)
         (do
           (println "song not in playlist, adding.")
           (-> db
               (update :playlist protocols/add-song song-name)))
         (do
           (println "song already in playlist, skipping")
           db))}))

(rf/reg-event-fx
 ::build-verified-playlist
 (fn-traced
  [{:keys [db]} _]
  (let [pl (pl/build-playlist
            (keys (get db :custom-song-delay {})))]
    {:db
     (-> db
         (assoc :playlist pl))
     :dispatch [::playlist-ready]})))

(rf/reg-event-db ::playlist-ready (fn-traced [db _] db))

(rf/reg-event-fx
 ::playlist-next
 (fn-traced
  [{:keys [db]} _]
  (let [new-db (-> db
                   (update :playlist protocols/next-song))]
    {:db new-db
     :dispatch [:cljs-karaoke.events.songs/navigate-to-song
                (protocols/current (:playlist new-db))]})))

(rf/reg-event-fx
 ::move-song-up
 (fn-traced
  [{:keys [db]} [_ pos]]
  {:db (-> db
           (update :playlist #(protocols/update-song-position % pos -1)))}))

(rf/reg-event-fx
 ::move-song-down
 (fn-traced
  [{:keys [db]} [_ pos]]
  {:db (-> db
           (update :playlist #(protocols/update-song-position % pos 1)))}))
