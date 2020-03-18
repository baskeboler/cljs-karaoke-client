(ns cljs-karaoke.subs
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.lyrics :as lyrics]
            [cljs-karaoke.protocols :as protocols]))

(defn reg-attr-sub [name key]
  (rf/reg-sub
   name
   (fn [db _]
     (get db key))))

(rf/reg-sub
 ::display-lyrics?
 (fn [db _]
   (:display-lyrics? db)))

(rf/reg-sub
 ::audio
 (fn-traced [db _]
            (:audio db)))

(rf/reg-sub
 ::audio-playback-rate
 :<- [::audio]
 (fn-traced
  [audio _]
  (.-playbackRate audio)))

(rf/reg-sub
 ::lyrics
 (fn-traced [db _]
            (:lyrics db)))

(rf/reg-sub
 ::lyrics-loaded?
 (fn-traced [db _]
            (:lyrics-loaded? db)))

(rf/reg-sub
 ::current-song-delay
 (fn-traced [db _]
            (get-in db [:custom-song-delay (:current-song db)])))


(rf/reg-sub
 ::current-frame
 :<- [::lyrics]
 :<- [::song-position]
 :<- [::current-song-delay]
 (fn-traced [[lyrics song-position custom-song-delay] _]
            (when-not (empty? lyrics)
              (reduce
               (fn [res frame]
                 (if (<= (protocols/get-offset frame) (+ (* -1 custom-song-delay) (* 1000 song-position)))
                   frame
                   res))
               nil
               (vec lyrics)))))
     ;; (last
      ;; (filterv
       ;; (fn [^cljs-karaoke.lyrics.LyricsFrame frame]
         ;; (<=
          ;; (protocols/get-offset frame)
          ;; (+ (* -1 custom-song-delay) (* 1000 song-position))
       ;; lyrics))))

(rf/reg-sub
 ::previous-frame
 :<- [::lyrics]
 :<- [::current-frame]
 (fn-traced
  [[lyrics frame] _]
  (let [frs (take-while #(not= (:id frame) (:id %)) lyrics)]
    (last frs))))

(rf/reg-sub
 ::next-frame
 :<- [::lyrics]
 :<- [::current-frame]
 (fn-traced
  [[lyrics frame] _]
  (let [frs (drop-while #(not= (:id frame) (:id %)) lyrics)]
    (second frs))))

(rf/reg-sub
 ::song-position-ms
 :<- [::song-position]
 (fn-traced
  [position _]
  (* 1000 position)))

(rf/reg-sub
 ::song-position-ms-adjusted
 :<- [::song-position-ms]
 :<- [::current-song-delay]
 (fn-traced
  [[position delay] _]
  (+ (* -1 delay)
     position)))


(rf/reg-sub
 ::next-lyrics-event
 :<- [::current-frame]
 :<- [::song-position]
 :<- [::current-song-delay]
 :<- [::next-frame]
 (fn-traced
  [[{:keys [events offset] :as frame} position delay next-frame] _]
  (let [position-ms   (+ (* -1 delay)
                         (* 1000 position)
                         (* -1 offset))
        next-in-frame (first
                       (filter #(> (:offset %) position-ms) events))]
    (if (some? next-in-frame)
      next-in-frame
      (first (:events next-frame))))))

(rf/reg-sub
 ::current-frame-done?
 :<- [::current-frame]
 :<- [::next-lyrics-event]
 (fn-traced
  [[{:keys [events offset] :as frame} evt] _]
  (not ((set events) evt))))

(rf/reg-sub
 ::current-song
 (fn-traced [db _]
            (:current-song db)))

(rf/reg-sub
 ::frame-to-display
 :<- [::current-frame]
 :<- [::next-frame]
 :<- [::current-frame-done?]
 :<- [::song-position-ms-adjusted]
 :<- [::current-song-delay]
 (fn-traced
  [[frame {:keys [next-offset] :as next-frame} done? position delay] _]
  (cond
    (not done?) frame
    (>= 1500 (-   next-offset position)) next-frame
    :else (if (some? frame) frame next-frame))))

(rf/reg-sub
 ::time-until-next-event
 :<- [::frame-to-display]
 :<- [::song-position-ms-adjusted]
 :<- [::next-lyrics-event]
 (fn-traced
  [[frame position evt] _]
  (-
   (+ (:offset frame) (:offset evt))
   position)))

(rf/reg-sub
 ::highlight-status
 (fn-traced [db _]
            (:highlight-status db)))

(rf/reg-sub
 ::lyrics-delay
 (fn-traced [db _]
            (:lyrics-delay db)))

(rf/reg-sub
 ::available-songs
 (fn-traced [db _]
            (:available-songs db)))

(rf/reg-sub
 ::song-list
 (fn-traced [db _]
            (:song-list db)))

(rf/reg-sub
 ::song-list-page-size
 :<- [::song-list]
 (fn-traced [song-list _]
            (:page-size song-list)))

(rf/reg-sub
 ::song-list-current-page
 :<- [::song-list]
 (fn-traced [song-list _]
            (:current-page song-list)))

(rf/reg-sub
 ::song-list-filter
 :<- [::song-list]
 (fn-traced [song-list _]
            (:filter song-list)))

(rf/reg-sub
 ::song-list-offset
 :<- [::song-list-current-page]
 :<- [::song-list-page-size]
 (fn-traced [[page size] _]
            (* page size)))

(rf/reg-sub
 ::song-list-visible?
 :<- [::song-list]
 (fn-traced [song-list _]
            (:visible? song-list)))

(rf/reg-sub
 ::song-list-filter-verified?
 :<- [::song-list]
 :<- [::verified-songs]
 (fn-traced [[song-list verified] _]
            (filter verified song-list)))

(rf/reg-sub
 ::song-position
 (fn-traced [db _]
            (:player-current-time db)))

(rf/reg-sub
 ::song-paused?
 (fn-traced [db _]
            (not (:playing? db))))

(rf/reg-sub
 ::song-duration
 (fn-traced [db _]
            (:song-duration db)))

(rf/reg-sub
 ::song-playing?
 (fn-traced [db _]
            (:playing? db)))

(rf/reg-sub
 ::custom-song-delay
 (fn-traced [db [_ song-name]]
            (get-in db [:custom-song-delay song-name] (:lyrics-delay db))))

(rf/reg-sub
 ::verified-songs
 (fn-traced [db _]
            (->> (keys (:custom-song-delay db))
                 (into #{}))))

(rf/reg-sub
 ::custom-song-delay-for-export
 (fn-traced [db _]
            (-> db
                :custom-song-delay
                (pr-str))))

(rf/reg-sub
 ::modals
 (fn-traced [db _]
            (:modals db)))

(rf/reg-sub
 ::bg-style
 (fn-traced [db _]
            (:bg-style db)))

(rf/reg-sub
 ::can-play?
 (fn-traced [db _]
            (:can-play? db)))

(rf/reg-sub
 ::current-view
 (fn-traced [db _]
            (:current-view db)))

(rf/reg-sub
 ::views
 (fn-traced [db _] (:views db)))

(rf/reg-sub
 ::view-property
 :<- [::views]
 (fn-traced [views [_ view property]]
            (get-in views [view property])))

(rf/reg-sub ::player-current-time (fn-traced [db _] (:player-current-time db)))
(rf/reg-sub ::audio-events (fn-traced [db _] (:audio-events db)))
(rf/reg-sub ::loop? (fn-traced [db _] (:loop? db)))
(rf/reg-sub ::app-name (fn-traced [db _] (:app-name db)))

(rf/reg-sub
 ::playlist
 (fn-traced [db _]
            (some-> db
                    :playlist)))
(rf/reg-sub
 ::playlist-current
 :<- [::playlist]
 (fn-traced [playlist _]
            (some-> playlist
                    (protocols/current))))
(rf/reg-sub
 ::navbar-visible?
 :<- [::current-view]
 (fn-traced [view _]
            (#{:playlist :home :editor} view)))

(rf/reg-sub
 ::initialized?
 (fn-traced [db _]
            (:initialized? db)))

(rf/reg-sub
 ::pageloader-active?
 (fn-traced [db _]
            (:pageloader-active? db)))

(rf/reg-sub
 ::pageloader-exiting?
 (fn-traced [db _]
            (:pageloader-exiting? db)))

(rf/reg-sub
 ::toasty?
 (fn-traced [db _]
            (:toasty? db)))

(rf/reg-sub
 ::stop-channel
 (fn-traced [db _]
            (:stop-channel db)))

(rf/reg-sub
 ::player-status-id
 (fn-traced [db _]
            (:player-status-id db)))

(rf/reg-sub
 ::seek-buttons-visible?
 (fn-traced [db _] (:seek-buttons-visible? db)))

(rf/reg-sub
 ::display-home-button?
 (fn-traced [db _] (:display-home-button? db)))

;; (rf/reg-sub
 ;; ::first-playback-position-updated?
 ;; (fn [db _] (:first-playback-position-updated? db)))

(rf/reg-sub
 ::notifications
 (fn-traced [db _]
            (:notifications db)))

(rf/reg-sub
 ::navbar-menu-active?
 (fn-traced [db _]
            (:navbar-menu-active? db)))

(rf/reg-sub
 ::song-frames-relative-positions
 :<- [::song-duration]
 :<- [::lyrics]
 :<- [::current-song-delay]
 (fn  [[duration frames delay] _]
   (let [duration-ms (* duration 1000)]
     (map (comp (partial * (/ 1.0 duration-ms))
                (partial + delay)
                protocols/get-offset)
          frames))))

(rf/reg-sub
 ::history
 (fn [db _] (:history db)))
