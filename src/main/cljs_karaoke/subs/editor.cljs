(ns cljs-karaoke.subs.editor
  (:require [re-frame.core :as rf]
            [clojure.string :as cstr]
            [cljs-karaoke.protocols :as p]
            [cljs-karaoke.subs.audio :as audio-subs]))
(rf/reg-sub
 ::editor-state
 (fn [db _]
   (:editor-state db)))

(rf/reg-sub
 ::current-frame
 :<- [::editor-state]
 (fn [editor-state _]
   (:current-frame editor-state)))

(rf/reg-sub
 ::song-name
 :<- [::editor-state]
 (fn [editor-state _]
   (:song-name editor-state)))

(rf/reg-sub
 ::current-frame-property
 :<- [::current-frame]
 (fn [current-frame [_ k]]
   (get current-frame k)))

(rf/reg-sub
 ::frames
 :<- [::editor-state]
 (fn [editor-state _]
   (:frames editor-state)))

(rf/reg-sub
 ::current-state
 :<- [::editor-state]
 (fn [editor-state _]
   (:current-state editor-state)))

(def mode-titles
  {:text-entry        "Frame text definition"
   :segment-selection "Divide text into segments"
   :segment-timing    "Synchronize segments with audio track"
   :frame-preview     "Preview work in progress"})

(rf/reg-sub
 ::mode-title
 :<- [::current-state]
 (fn [current-state _]
   (mode-titles current-state)))

(rf/reg-sub
 ::segments-ready?
 :<- [::editor-state]
 (fn [{:keys [current-frame]} _]
   (= (reduce + (:segment-sizes current-frame))
      (count (:text current-frame)))))

(rf/reg-sub
 ::segment-timings-ready?
 :<- [::editor-state]
 (fn [{:keys [current-frame]} _]
   (= (count (:segment-offsets current-frame))
      (count (:segments current-frame)))))

(rf/reg-sub
 ::active-segment
 :<- [::current-frame-property :segments]
 :<- [::current-frame-property :segment-offsets]
 :<- [:cljs-karaoke.subs/song-position-ms]
 (fn [[segments offsets position] _]
   (->> (vec (vals segments))
        (mapv (fn [o s] (merge s {:offset o})) offsets)
        (filterv #(<= (:offset %) position))
        (reduce (fn [res o]
                  (cond
                    (nil? res)                    o
                    (> (:offset o) (:offset res)) o
                    :otherwise                    res))))))

(rf/reg-sub
 ::active-frame
 :<- [::frames]
 :<- [:cljs-karaoke.subs/song-position-ms]
 (fn [[frames position] _]
   (reduce
    (fn [res f]
      (cond
        (nil? res)                     f
        (and (> (:offset f) (:offset res))
             (> position (:offset f))) f
        :otherwise                     res))
    frames)))

(rf/reg-sub
 ::frame-count
 :<- [::frames]
 (fn [frames _]
   (count frames)))

(defn flip [function]
  (fn
    ([] function)
    ([x] (function x))
    ([x y] (function y x))
    ([x y z] (function z y x))
    ([x y z w] (function w z y x))
    ([x y z w & rest] (->> rest
                           (concat [x y z w])
                           reverse
                           (apply function)))))

(rf/reg-sub
 ::word-count
 :<- [::frames]
 (fn [frames _]
   (->> frames
        (mapv :events)
        (mapv (fn [events]
                (->> events
                     (mapv :text)
                     (apply str)
                     ((flip cstr/split) #" ")
                     (count))))
        (reduce + 0))))

(rf/reg-sub
 ::frame-count
 :<- [::frames]
 (fn [frames _]
   (count frames)))

(rf/reg-sub
 ::words-per-frame
 :<- [::word-count]
 :<- [::frame-count]
 (fn [[word-count frame-count] _]
   (/ word-count frame-count)))
