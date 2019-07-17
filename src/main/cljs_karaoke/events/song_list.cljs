(ns cljs-karaoke.events.song-list
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def initial-song-list-state
  {:page-size 10
   :current-page 0
   :filter ""
   :filter-verified? false
   :visible? true})

(rf/reg-event-fx
 ::init-song-list-state
 (fn-traced
  [{:keys [db]} _]
  {:db (-> db
           (assoc :song-list initial-song-list-state))
   :dispatch [::song-list-ready]}))


(rf/reg-event-db
 ::set-song-list-current-page
 (fn-traced [db [_ page]]
            (-> db
                (assoc-in [:song-list :current-page] page))))

(rf/reg-event-db
 ::toggle-song-list-visible
 (fn-traced
  [db _]
  (-> db
      (update-in [:song-list :visible?] not))))

(rf/reg-event-db
 ::toggle-filter-verified-songs
 (fn-traced
  [db _]
  (-> db
      (update-in [:song-list :filter-verified?] not))))


(rf/reg-event-fx
 ::set-song-filter
 (fn-traced
  [{:keys [db]} [_ filter-text]]
  {:db (-> db
           (assoc-in [:song-list :filter] filter-text))
   :dispatch [::set-song-list-current-page 0]})) 

(rf/reg-event-db
 ::song-list-ready
 (fn-traced
  [db _]
  (. js/console (log "song list ready"))
  (-> db
      (assoc-in [:song-list :ready?] true))))
