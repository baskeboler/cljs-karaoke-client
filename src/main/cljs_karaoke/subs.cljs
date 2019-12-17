(ns cljs-karaoke.subs
  (:require [re-frame.core :as rf :include-macros true]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.lyrics :as lyrics]
            [cljs-karaoke.protocols :as protocols]))
(rf/reg-sub
 ::display-lyrics?
 (fn [db _]
   (:display-lyrics? db)))

(rf/reg-sub
 ::audio
 (fn [db _]
   (:audio db)))

(rf/reg-sub
 ::lyrics
 (fn [db _]
   (:lyrics db)))

(rf/reg-sub
 ::lyrics-loaded?
 (fn [db _]
   (:lyrics-loaded? db)))

(rf/reg-sub
 ::current-song-delay
 (fn [db _]
   (get-in db [:custom-song-delay (:current-song db)])))
(rf/reg-sub
 ::current-frame
 :<- [::lyrics]
 :<- [::song-position]
 :<- [::custom-song-delay]
 (fn [[lyrics song-position custom-song-delay] _]
   (when-not (or
              (empty? lyrics)
              (nil? song-position)
              (zero? song-position))
     (last
      (filterv
       (fn [^cljs-karaoke.lyrics.LyricsFrame frame]
         (< 
          (protocols/get-offset frame)
          (+ (* -1 custom-song-delay) (* 1000 song-position))))
       lyrics)))))

(rf/reg-sub
 ::current-song
 (fn [db _]
   (:current-song db)))

;; (rf/reg-sub
 ;; ::player-status
 ;; (fn [db _]
   ;; (:player-status db)))

(rf/reg-sub
 ::highlight-status
 (fn [db _]
   (:highlight-status db)))

(rf/reg-sub
 ::lyrics-delay
 (fn [db _]
   (:lyrics-delay db)))


(rf/reg-sub
 ::song-list
 (fn [db _]
   (:song-list db)))

(rf/reg-sub
 ::song-list-page-size
 :<- [::song-list]
 (fn [song-list _]
   (:page-size song-list)))

(rf/reg-sub
 ::song-list-current-page
 :<- [::song-list]
 (fn [song-list _]
   (:current-page song-list)))

(rf/reg-sub
 ::song-list-filter
 :<- [::song-list]
 (fn [song-list _]
   (:filter song-list)))

(rf/reg-sub
 ::song-list-offset
 :<- [::song-list-current-page]
 :<- [::song-list-page-size]
 (fn [[page size] _]
   (* page size)))

(rf/reg-sub
 ::song-list-visible?
 :<- [::song-list]
 (fn [song-list _]
   (:visible? song-list)))

(rf/reg-sub
 ::song-list-filter-verified?
 :<- [::song-list]
 :<- [::verified-songs]
 (fn [[song-list verified] _]
   (filter verified song-list)))

(rf/reg-sub
 ::song-position
 (fn [db _]
   (:player-current-time db)))
(rf/reg-sub
 ::song-paused?
 (fn [db _]
   (not (:playing? db))))
(rf/reg-sub
 ::song-duration
 (fn [db _]
   (:song-duration db)))

(rf/reg-sub
 ::song-playing?
 (fn [db _]
   (:playing? db)))

(rf/reg-sub
 ::custom-song-delay
 (fn [db [_ song-name]]
   (get-in db [:custom-song-delay song-name] (:lyrics-delay db))))

(rf/reg-sub
 ::verified-songs
 (fn [db _]
   (->> (keys (:custom-song-delay db))
        (into #{}))))

(rf/reg-sub
 ::custom-song-delay-for-export
 (fn [db _]
   (-> db
       :custom-song-delay
       (pr-str))))

(rf/reg-sub
 ::modals
 (fn [db _]
   (:modals db)))

(rf/reg-sub
 ::bg-style
 (fn [db _]
   (:bg-style db)))

(rf/reg-sub
 ::can-play?
 (fn [db _]
   (:can-play? db)))

(rf/reg-sub
 ::current-view
 (fn [db _]
   (:current-view db)))

(rf/reg-sub
 ::views
 (fn [db _] (:views db)))

(rf/reg-sub
 ::view-property
 :<- [::views]
 (fn [views [_ view property]]
   (get-in views [view property])))

(rf/reg-sub ::player-current-time (fn [db _] (:player-current-time db)))
(rf/reg-sub ::audio-events (fn [db _] (:audio-events db)))
(rf/reg-sub ::loop? (fn [db _] (:loop? db)))


(rf/reg-sub
 ::playlist
 (fn [db _]
   (some-> db
           :playlist)))
(rf/reg-sub
 ::playlist-current
 :<- [::playlist]
 (fn [playlist _]
   (some-> playlist
           (protocols/current))))
(rf/reg-sub
 ::navbar-visible?
 :<- [::current-view]
 (fn [view _]
   (#{:playlist :home} view)))
;; (rf/reg-sub
 ;; ::initialized?
 ;; :<- [::views]
 ;; :<- [::song-list]
 ;; (fn [[views songs] _]
   ;; (and
    ;; (not (empty? @re-frame.db/app-db))
    ;; (not (empty? views))
    ;; (not (empty? songs)))))
    ;; (not (nil? (:playlist @re-frame.db/app-db))))))
(rf/reg-sub
 ::initialized?
 (fn [db _]
   (:initialized? db)))

(rf/reg-sub
 ::pageloader-active?
 (fn [db _]
   (:pageloader-active? db)))

(rf/reg-sub
 ::toasty?
 (fn [db _]
   (:toasty? db)))

(rf/reg-sub
 ::stop-channel
 (fn [db _]
   (:stop-channel db)))

(rf/reg-sub
 ::player-status-id
 (fn [db _]
   (:player-status-id db)))

(rf/reg-sub
 ::seek-buttons-visible?
 (fn [db _] (:seek-buttons-visible? db)))

(rf/reg-sub
 ::display-home-button?
 (fn [db _] (:display-home-button? db)))

;; (rf/reg-sub
 ;; ::first-playback-position-updated?
 ;; (fn [db _] (:first-playback-position-updated? db)))

(rf/reg-sub
 ::notifications
 (fn [db _]
   (:notifications db)))

(rf/reg-sub
 ::navbar-menu-active?
 (fn [db _]
   (:navbar-menu-active? db)))
