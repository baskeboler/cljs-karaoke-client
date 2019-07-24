(ns cljs-karaoke.events.common
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(defn reg-set-attr [evt-name attr-name]
  (rf/reg-event-db
   evt-name
   (fn-traced [db [_ obj]]
              (assoc db attr-name obj))))

(defn save-custom-delays-to-localstore [delays]
  (. js/localStorage (setItem "custom-song-delays" (js/JSON.stringify (clj->js delays)))))

(defn get-custom-delays-from-localstorage []
  (-> (. js/localStorage (getItem "custom-song-delays"))
      (js/JSON.parse)
      (js->clj)))

(defn save-to-localstore [name obj]
  (. js/localStorage (setItem name (js/JSON.stringify (clj->js obj)))))

(defn get-from-localstorage [name]
  (-> (. js/localStorage (getItem name))
      (js/JSON.parse)
      (js->clj)))

(defn set-location-href [url]
  (set! (.-href js/location) url))

(defn set-location-hash [path]
  (set! (.-hash js/location) (str "#" path)))

(rf/reg-event-fx
 ::save-to-localstorage
 (rf/after
  (fn [_ [_ name obj cbevent]]
    (. js/console (log "Saving to localstorage: " name " - " (clj->js obj)))
    (save-to-localstore name obj)))
 (fn-traced
  [{:keys [db]} [_ name obj callback-event]]
  (merge
   {:db db}
   (if-not (nil? callback-event)
     {:dispatch [callback-event]}
     {}))))

(defn set-page-title! [title]
  (set! (.-title js/document) title))

(rf/reg-event-db
 ::set-page-title
 (rf/after
  (fn [_ [_ title]]
    (set-page-title! title)))
 (fn-traced
  [db [_ title]]
  (-> db (assoc :page-title title))))

