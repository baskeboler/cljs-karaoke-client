(ns cljs-karaoke.protocols)


(defprotocol ^:export LyricsDisplay
  (set-text  [self t])
  (reset-progress [self])
  (inc-progress [self])
  (get-progress [self]))


(defprotocol ^:export PLyrics
  (get-text [this])
  (get-offset [this])
  (played? [this offset])
  (get-next-event [this offset]))

(defprotocol ^:export PLyricsStats
  (get-frame-count [this])
  (get-word-count [this])
  (get-avg-words-per-frame [this])
  (get-max-words-frame [this])
  (get-min-words-frame [this]))

(defprotocol ^:export PSong
  (get-current-frame [this time] "returns the current frame a time miliseconds"))


(defprotocol ^:export Playlist
  (add-song [this song])
  (remove-song [this pos])
  (next-song [this])
  (clear [this])
  (is-empty? [this])
  (current [this])
  (has-next? [this])
  (contains-song? [this song-name])
  (songs [this])
  (update-song-position [this pos dpos]))

(defprotocol ^:export Storable
  (to-json [this])
  (from-json [this json]))


(defmulti handle-route "route handling multimethod" :action)
(defmethod handle-route :default [arg] (:action arg))

(defprotocol ViewDispatcher
  "View dispatcher protocol"
  (dispatch-view [this]))
