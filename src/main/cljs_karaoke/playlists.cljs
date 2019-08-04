(ns cljs-karaoke.playlists)

(def empty-col? cljs.core/empty?)

(defprotocol ^:export Playlist
  (add-song [this song])
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

(defrecord ^:export KaraokePlaylist [id created current songs]
  Playlist
  (songs [this] songs)
  (add-song [this song] (-> this (update :songs conj song)))
  (next-song [this]
    (if (has-next? this)
      (-> this
          (update :current inc))
      (-> this
          (assoc :current 0))))
  (clear [this] (-> this
                    (assoc :current 0)
                    (assoc :songs [])))
  (is-empty? [this]
    (empty-col? (:songs this)))
  (current [this]
    (if (< (:current this) (count (:songs this)))
      (nth
       (:songs this)
       (:current this))
      nil))
  (has-next? [this]
    (< (inc (:current this)) (count songs)))
  (contains-song? [this song-name]
    (contains? songs song-name))
  Storable
  (to-json [this]
    (let [o           {:id (:id this)
                       :created (:created this)
                       :current (:current this)
                       :songs (:songs this)}]
      (js/JSON.stringify (clj->js o))))
  (from-json [this json]
    this))

(defn build-playlist
  ([id created current songs] (->KaraokePlaylist id created current (vec songs)))
  ([created current songs] (build-playlist (str (random-uuid)) created current songs))
  ([current songs] (build-playlist (js/Date.) current songs))
  ([songs] (build-playlist 0 songs))
  ([] (build-playlist [])))
