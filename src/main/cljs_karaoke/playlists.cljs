(ns cljs-karaoke.playlists
  (:require [cljs-karaoke.protocols :as protocols
             :refer [Playlist Storable add-song remove-song next-song
                     clear is-empty? current has-next? contains-song?
                     songs update-song-position to-json from-json]]))
(def empty-col? cljs.core/empty?)


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
  (update-song-position [this pos d]
    (-> this
        (update :songs (fn [song-list]
                         (let [new-pos (+ pos d)
                               new-pos (cond
                                         (< new-pos 0) 0
                                         (> new-pos (count song-list)) (count song-list)
                                         :else new-pos)
                               moved (nth song-list pos)
                               song-list (->> (map vector (range) song-list)
                                              (filter
                                               (fn [[i song]]
                                                 (not= i pos)))
                                              (map second))]
                           (->> (concat (take new-pos song-list)
                                        [moved]
                                        (drop new-pos song-list))
                                (into [])))))
        (update :current (fn [c] (if (= pos c)
                                  (+ pos d)
                                  c)))))
  (remove-song [this pos]
    (-> this
        (update :songs (fn [song-list]
                         (->> (map vector (range) song-list)
                              (filter (fn [[i song]]
                                        (not= i pos)))
                              (mapv second))))))
  
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
