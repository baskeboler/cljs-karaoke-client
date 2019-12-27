(ns cljs-karaoke.events.editor
  (:require [reagent.core :as reagent]
            [clojure.string :as cstr]
            [cljs-karaoke.lyrics :as lyrics]
            [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))
(def initial-state
  {:frames        []
   :current-frame {:text            "hola como te va?"
                   :text-done?      false
                   :segments-done?  false
                   :offsets-done?   false
                   :segment-sizes   []
                   :segment-offsets []
                   :segments        []
                   :segment-size     0}})

(rf/reg-event-db
 ::init
 (fn-traced
  [db _]
  (-> db
      (assoc-in [:editor-state] initial-state))))

(rf/reg-event-db
 ::reset-frame
 (fn-traced
  [db _]
  (-> db
      (assoc-in [:editor-state :current-frame] (:current-frame initial-state)))))
(rf/reg-event-db
 ::add-frame
 (fn-traced
  [db _]
  (let [events (map lyrics/create-lyrics-event (-> db :editor-state :current-frame :segments vals))
        frame (lyrics/->LyricsFrame (str (random-uuid)) events :frame-event -1 (-> events first :offset))]
   (-> db
       (update-in [:editor-state :frames] conj frame)))))
(rf/reg-event-db
 ::set-current-frame-property
 (fn-traced
  [db [_ k v]]
  (-> db
      (assoc-in [:editor-state :current-frame k] v))))
 
                 
(defn new-frame [text timestamp-ms]
  {:offset timestamp-ms
   :text text
   :events []})

(defn get-segment [n text]
  [(apply str (take n text)) (apply str (drop n text))])

(defn get-segments [sizes text]
  (let [res   (reduce
               (fn [res s]
                 (let [remaining (last res)
                       new-rem   (get-segment s remaining)]
                   (concat
                    (butlast res)
                    new-rem)))
               [text]
               sizes)
        done? (cstr/blank? (last res))]
    {:result         (butlast res)
     :done?          done?
     :text           text
     :remaining-text (apply str (drop (reduce + sizes) text))}))

(defn mark-segment-offset [timestamp-ms segments]
  [{:text   (first segments)
    :offset timestamp-ms}
   (rest segments)])

(defn mark-segments-offset [timestamps-ms segments]
  (let [res   (reduce
               (fn [res t]
                 (let [remaining (last res)
                       new-rem   (mark-segment-offset t remaining)]
                   (concat
                    (butlast res)
                    new-rem)))
               [segments]
               timestamps-ms)
        done? (empty? (last res))]
    {:result             (butlast res)
     :done?              done?
     :text               (apply str segments)
     :remaining-segments (last res)}))
