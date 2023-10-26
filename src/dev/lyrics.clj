(ns dev.lyrics
  (:require [tick.core :as t]))


(defn convert-to-new-song-format [& {:keys [title frames]}]
  (assert (seq? frames))
  {:title         title
   :frames        frames
   :date          (str (t/now))
   :tempo-bpm     -1
   :division-type -1
   :resolution    -1
   :type          :song-data})


(defn is-old-format? [song]
  (seq? song))
