(ns cljs-karaoke.subs
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.lyrics :as lyrics]
            [clj-karaoke.song-data]
            [clj-karaoke.lyrics-event]
            [clj-karaoke.lyrics-frame]
            [cljs-karaoke.protocols :as protocols]
            [cljs-karaoke.subs.common :refer [reg-attr-sub]]
            [clj-karaoke.protocols :as p]))
(rf/reg-sub
 ::display-lyrics?
 (fn [db _]
   (:display-lyrics? db)))

(rf/reg-sub
 ::audio
 (fn-traced [db _]
            (:audio db)))
(rf/reg-sub
 ::effects-audio
 (fn-traced [db _]
            (:effects-audio db)))

(rf/reg-sub
 ::audio-playback-rate
 :<- [::audio]
 (fn-traced
  [audio _]
  (.-playbackRate audio)))

(rf/reg-sub
 ::song
 (fn-traced
  [db _]
  (:song db)))

(rf/reg-sub
 ::lyrics
 :<- [::playback-song]
 (fn-traced
  [song _]
  (vec (:frames song))))

(rf/reg-sub
 ::lyrics-loaded?
 (fn-traced [db _]
            (:lyrics-loaded? db)))

(rf/reg-sub
 ::current-song-delay
 (fn-traced
  [db _]
  (:lyrics-delay db)))
  ;; (get-in db [:custom-song-delay (:current-song db)])))

(rf/reg-sub
 ::all-song-delays
 (fn-traced
  [db _]
  (:custom-song-delay db)))

(rf/reg-sub
 ::song-delay
 :<- [::all-song-delays]
 (fn-traced
  [all-delays [_ song-name]]
  (get-in all-delays [song-name] 0)))

(rf/reg-sub
 ::has-delay?
 :<- [::all-song-delays]
 (fn-traced
  [all-delays [_ song-name]]
  (some?
   (get all-delays song-name nil))))

(rf/reg-sub
 ::current-frame-backup
 :<- [::lyrics]
 :<- [::song-position]
 :<- [::current-song-delay]
 (fn
   [[lyrics song-position custom-song-delay] _]
   (when-not (empty? lyrics)
     (reduce
      (fn [res frame]
        (if (<= (p/get-offset frame)
                (+ (* -1 custom-song-delay)
                   (* 1000 song-position)))
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
 ::current-frame
 :<- [::playback-song]
 ;; :<- [::song]
 :<- [::song-position-ms-adjusted]
 (fn
   [[song song-position] _]
   (p/get-current-frame song song-position)))
  ;; (protocols/get-current-frame song song-position)))
  ;; (let [lyrics frames]
  ;;  (when-not (empty? lyrics)
  ;;    (reduce
  ;;     (fn [res frame]
  ;;       (if (<= (protocols/get-offset frame)
  ;;               (+ (* -1 custom-song-delay)
  ;;                  (* 1000 song-position)))
  ;;         frame
  ;;         res))
  ;;     nil
  ;;     (vec lyrics))))))
(rf/reg-sub
 ::previous-frame
 :<- [::lyrics]
 :<- [::current-frame]
 (fn
   [[lyrics frame] _]
   (let [frs (take-while #(not= (:id frame) (:id %)) lyrics)]
     (last frs))))

(rf/reg-sub
 ::next-frame
 :<- [::lyrics]
 :<- [::song-position-ms-adjusted]
 (fn
   [[lyrics pos] _]
   (let [frs (drop-while #(p/played? % pos) lyrics)]
     (if (> (count frs) 1)
       (second frs)
       (first frs)))))

(rf/reg-sub
 ::song-position-ms
 :<- [::song-position]
 (fn
   [position _]
   (* 1000 position)))

(rf/reg-sub
 ::song-position-ms-adjusted
 :<- [::song-position-ms]
 :<- [::current-song-delay]
 (fn
   [[position delay] _]
   (+ (* -1 delay)
      position)))


;; (rf/reg-sub
;;  ::next-lyrics-event-old
;;  :<- [::current-frame]
;;  :<- [::song-position]
;;  :<- [::current-song-delay]
;;  :<- [::next-frame]
;;  (fn
;;   [[{:keys [events offset] :as frame} position delay next-frame] _]
;;   (let [position-ms   (+ (* -1 delay)
;;                          (* 1000 position)
;;                          (* -1 offset))
;;         next-in-frame (first
;;                        (filter #(> (:offset %) position-ms) events))]
;;     (if (some? next-in-frame)
;;       next-in-frame
;;       (first (:events next-frame))))))


(rf/reg-sub
 ::next-lyrics-event
 :<- [::current-frame]
 :<- [::song-position-ms-adjusted]
 :<- [::next-frame]
 (fn
   [[{:keys [events offset] :as frame} position next-frame] _]
   (if-not (p/played? frame position)
     (p/get-next-event frame position)
     (when-not (nil? next-frame)
       (-> next-frame :events first)))))
  ;; (let [next-in-frame (first
                       ;; (filter #(> (+ offset (:offset %)) position) events))
    ;; (if (some? next-in-frame)
      ;; next-in-frame
      ;; (first (:events next-frame))))))

(rf/reg-sub
 ::current-frame-done?
 :<- [::current-frame]
 :<- [::next-lyrics-event]
 :<- [::song-position-ms-adjusted]
 (fn
   [[frame evt pos] _]
  ;; (not ((set events) evt))))
   (p/played? frame pos)))

(rf/reg-sub
 ::current-song
 (fn [db _]
   (:current-song db)))

(rf/reg-sub
 ::frame-to-display
 :<- [::current-frame]
 :<- [::next-frame]
 :<- [::current-frame-done?]
 :<- [::song-position-ms-adjusted]
 ;; :<- [::current-song-delay]
 (fn
   [[frame  next-frame done? position] _]
   (cond
     (not (p/played? frame position)) frame
     (>= 1500 (- (p/get-offset next-frame) position)) next-frame
     :else
     frame)))
    ;; (if (some? frame) frame next-frame))))

(rf/reg-sub
 ::time-until-next-event
 :<- [::frame-to-display]
 :<- [::song-position-ms-adjusted]
 :<- [::next-lyrics-event]
 (fn
   [[frame position evt] _]
   (-
    (+ (p/get-offset frame) (p/get-offset evt))
   ;; (+ (:offset frame) (:offset evt))
    position)))

(rf/reg-sub
 ::highlight-status
 (fn [db _]
   (:highlight-status db)))

(rf/reg-sub
 ::lyrics-delay
 (fn [db _]
   (:lyrics-delay db)))

(rf/reg-sub
 ::available-songs
 (fn [db _]
   (:available-songs db)))

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
 ::user-custom-song-delay-map
 (fn
   [db _]
   (:user-custom-song-delay db)))

(rf/reg-sub
 ::custom-song-delay-map
 (fn
   [db _]
   (:custom-song-delay db)))

(rf/reg-sub
 ::custom-song-delay
 :<- [::custom-song-delay-map]
 :<- [::user-custom-song-delay-map]
 (fn
   [[delay-map user-delay-map] [_ song-name]]
   (let [final-delay-map (merge delay-map user-delay-map)]
     (get-in final-delay-map [song-name] 0))))

(rf/reg-sub
 ::user-song-delay-count
 :<- [::user-custom-song-delay-map]
 (fn
   [delays _]
   (-> delays keys count)))

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
(rf/reg-sub ::app-name (fn [db _] (:app-name db)))

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
   (#{:playlist :home :editor} view)))

(rf/reg-sub
 ::backgrounds-loaded?
 (fn
   [db _]
   (:song-backgrounds-loaded? db)))

(rf/reg-sub
 ::song-delays-loaded?
 (fn
   [db _]
   (:song-delays-loaded db)))

(rf/reg-sub
 ::views-state-ready?
 (fn
   [db _]
   (:views-state-ready? db)))

(rf/reg-sub
 ::initialized?
 (fn [db _]
   (:initialized? db)))

(rf/reg-sub
 ::app-ready?
 :<- [::initialized?]
 :<- [::backgrounds-loaded?]
 :<- [::song-delays-loaded?]
 :<- [::views-state-ready?]
 (fn
   [[initialized? bgloaded? delaysloaded? vsready?] _]
   (and initialized? bgloaded? delaysloaded? vsready?)))

(rf/reg-sub
 ::pageloader-active?
 (fn [db _]
   (:pageloader-active? db)))

(rf/reg-sub
 ::pageloader-exiting?
 (fn [db _]
   (:pageloader-exiting? db)))

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

(rf/reg-sub
 ::song-frames-relative-positions
 :<- [::song-duration]
 :<- [::lyrics]
 :<- [::lyrics-delay]
                                        ;:<- [::current-song-delay]
 (fn  [[duration frames delay] _]
   (let [duration-ms (* duration 1000)]
     (map (comp (partial * (/ 1.0 duration-ms))
                (partial + delay)
                p/get-offset)
          frames))))

(rf/reg-sub
 ::history
 (fn [db _] (:history db)))

(rf/reg-sub
 ::song-backgrounds
 (fn [db _] (:song-backgrounds db)))

(rf/reg-sub
 ::song-metadata
 (fn [db _]
   (:song-metadata db)))

(rf/reg-sub
 ::current-song-metadata
 :<- [::current-song]
 :<- [::song-metadata]
 (fn [[current-song metadata] _]
   (get metadata current-song)))

(rf/reg-sub
 ::playback-song
 ;; :<- [::current-song]
 ;; :<- [::lyrics]
 (fn [db _]
   (:song db)))
   ;; (lyrics/create-song song-name frames)))

(rf/reg-sub
 ::current-song-frame-count
 :<- [::playback-song]
 (fn [song _]
   (protocols/get-frame-count song)))

(rf/reg-sub
 ::current-song-word-count
 :<- [::playback-song]
 (fn [song _]
   (protocols/get-word-count song)))

(rf/reg-sub
 ::current-song-avg-words-frame
 :<- [::playback-song]
 (fn [song _]
   (protocols/get-avg-words-per-frame song)))

(rf/reg-sub
 ::current-song-max-words-frame
 :<- [::playback-song]
 (fn [song _]
   (protocols/get-max-words-frame song)))

(rf/reg-sub
 ::current-song-min-words-frame
 :<- [::playback-song]
 (fn [song _]
   (protocols/get-min-words-frame song)))

(rf/reg-sub
 ::new-songs
 (fn [db _]
   (:new-songs db)))

(rf/reg-sub
 ::new-song?
 :<- [::new-songs]
 (fn [songs [_ song]]
   (songs song)))
