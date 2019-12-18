(ns cljs-karaoke.protocols)


(defprotocol ^:export LyricsDisplay
  (set-text  [self t])
  (reset-progress [self])
  (inc-progress [self])
  (get-progress [self]))


(defprotocol ^:export PLyrics
  (get-text [this])
  (get-offset [this])
  (played? [this offset]))


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
