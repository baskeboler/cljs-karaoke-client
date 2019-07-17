(ns cljs-karaoke.events.playlists
  (:require [re-frame.core :as rf :include-macros true]
            [cljs-karaoke.playlists :as pl]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))


(rf/reg-event-fx
 ::playlist-load
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch-later [{:ms 2000
                     :dispatch [::set-current-playlist-song]}]}))

(rf/reg-event-fx
 ::set-current-playlist-song
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch (if-not (nil? (:playlist db))
               [:cljs-karaoke.events/set-current-song (pl/current (:playlist db))]
               [::playlist-load])}))


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
                   (update :playlist pl/next-song))]
    {:db new-db
     :dispatch [:cljs-karaoke.events.songs/trigger-load-song-flow
                (pl/current (:playlist new-db))]})))
