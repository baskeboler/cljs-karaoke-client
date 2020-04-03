(ns cljs-karaoke.editor.events
  (:require [clojure.string :as cstr]
            [cljs-karaoke.lyrics :as lyrics]
            [cljs-karaoke.events.common :as common-events]
            [cljs.tools.reader :as reader]
            [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))
(def editor-states #{:text-entry :segment-selection :segment-timing :frame-preview})
(def editor-actions #{:reset :confirm-text :confirm-segments :confirm-timing :select-frame :preview-frame})
(def editor-transitions
  {:text-entry        {:confirm-text :segment-selection
                       :reset        :text-entry}
   :segment-selection {:reset            :text-entry
                       :confirm-segments :segment-timing}
   :segment-timing    {:reset          :text-entry
                       :confirm-timing :text-entry
                       :preview-frame  :frame-preview}
   :frame-preview     {:reset         :text-entry
                       :review-timing :segment-timing}})
(defn perform-transition [current-state action]
  (assert (editor-states current-state))
  (assert (editor-actions action))
  (get-in editor-transitions [current-state action] current-state))

(def initial-state
  {:current-state :text-entry
   :frames        []
   :song-name     ""
   :current-frame {:text            "hola como te va?"
                   :text-done?      false
                   :mode            :creating
                   :id              nil
                   :segments-done?  false
                   :offsets-done?   false
                   :segment-sizes   []
                   :segment-offsets []
                   :segments        []
                   :segment-size    0}})

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
 ::editor-action
 (fn-traced
  [db [_ action]]
  (assert (editor-actions action))
  (-> db
      (update-in [:editor-state :current-state] perform-transition action))))
      
(rf/reg-event-db
 ::delete-frame
 (fn-traced
  [db [_ frame-id]]
  (-> db
      (update-in [:editor-state :frames]
                 (fn [frames]
                   (filter #(not= frame-id (:id %)) frames))))))
(rf/reg-event-fx
 ::add-frame
 (fn-traced
  [{:keys [db]} _]
  (let [offsets      (get-in db [:editor-state :current-frame :segment-offsets] [])
        frame-id     (get-in db [:editor-state :current-frame :id])
        frame-id     (if (nil? frame-id)
                       (str (random-uuid))
                       frame-id)
        segments     (vals (get-in db [:editor-state :current-frame :segments]))
        segments     (map merge segments (map (fn [o] {:offset o}) offsets))
        events       (map lyrics/create-lyrics-event segments)
        frame-offset (-> events first :offset)
        frame        (lyrics/->LyricsFrame frame-id
                                           (map #(update % :offset (fn [off] (- off frame-offset))) events)
                                           :frame-event
                                           -1
                                           frame-offset)]
    {:db       (-> db
                   (update-in [:editor-state :frames] conj frame)
                   (update-in [:editor-state :frames] #(sort-by :offset %)))
     :dispatch [::reset-frame]})))

(rf/reg-event-fx
 ::load-frames
 (fn-traced
  [{:keys [db]} [_ text]]
  (let [the-frames (reader/read-string text)
        the-frames (mapv lyrics/create-frame the-frames)]
    {:db (-> db (assoc-in [:editor-state :frames] the-frames))
     :dispatch [::reset-frame]})))
(defn- event->segment [evt]
  {:id     (:id evt)
   :offset (:offset evt)
   :text   (:text evt)})

(common-events/reg-set-attr ::set-song-name [:editor-state :song-name])

(rf/reg-event-fx
 ::edit-frame
 (fn-traced
  [{:keys [db]} [_ frame-id]]
  (let [frames (get-in db [:editor-state :frames])
        frame  (first (filter #(= frame-id (:id %)) frames))]
    (if-not (nil? frame)
      (let [events   (mapv #(update % :offset (partial + (:offset frame))) (:events frame))
            events   (sort-by :offset events)
            events   (map-indexed (fn [i evt] (assoc evt :id i)) events)
            offsets  (mapv :offset events)
            ;; offsets  (mapv (partial + (:offset frame)) offsets)
            segments (into (sorted-map)
                           (for [seg (mapv event->segment events)]
                             [(:id seg) (dissoc seg :offset)]))
            sizes    (mapv #(count (:text %)) (vals segments))]
        {:db       (-> db
                       (assoc-in [:editor-state :current-frame]
                                 {:segments        segments
                                  :segment-offsets offsets
                                  :segment-sizes   sizes
                                  :segment-size    0
                                  :mode            :editing
                                  :text-done?      true
                                  :segments-done?  true
                                  :offsets-done?   true 
                                  :text            (apply str (mapv :text events))
                                  :id              frame-id})
                       (assoc-in [:editor-state :current-state] :segment-timing))
         :dispatch [::delete-frame frame-id]})
      {:db db}))))
(rf/reg-event-db
 ::set-current-frame-property
 (fn-traced
  [db [_ k v]]
  (-> db
      (assoc-in [:editor-state :current-frame k] v))))
 
                 
(defn new-frame [text timestamp-ms]
  {:offset timestamp-ms
   :text   text
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
