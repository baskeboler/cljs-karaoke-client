(ns cljs-karaoke.subs.editor
  (:require [re-frame.core :as rf]
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
