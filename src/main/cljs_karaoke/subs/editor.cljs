(ns cljs-karaoke.subs.editor
  (:require [re-frame.core :as rf]))

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
