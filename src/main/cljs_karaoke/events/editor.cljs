(ns cljs-karaoke.events.editor
  (:require [reagent.core :as reagent]
            [clojure.string :as cstr]))

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
