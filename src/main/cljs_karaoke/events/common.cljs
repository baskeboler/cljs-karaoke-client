(ns cljs-karaoke.events.common
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [reagent.core :as reagent :refer [atom]]))

(defn reg-set-attr [evt-name attr-name]
  (cond
    (keyword? attr-name)
    (rf/reg-event-db
     evt-name
     (fn-traced
      [db [_ obj]]
      (assoc db attr-name obj)))
    (vector? attr-name)
    (rf/reg-event-db
     evt-name
     (fn-traced
      [db [_ obj]]
      (-> db
          (assoc-in attr-name obj))))))
(defn save-to-localstorage [name obj]
  (when-not (or
             (nil? name)
             (empty? name)
             (nil? obj))
    (. js/localStorage (setItem name (js/JSON.stringify (clj->js obj))))))

(defn get-from-localstorage [name]
  (-> (. js/localStorage (getItem name))
      (js/JSON.parse)
      (js->clj)))

(defn save-custom-delays-to-localstore [delays]
  ;; (. js/localStorage (setItem "custom-song-delays" (js/JSON.stringify (clj->js delays)))))
  (save-to-localstorage "custom-song-delays" delays))

(defn get-custom-delays-from-localstorage []
  ;; (-> (. js/localStorage (getItem "custom-song-delays"))
      ;; (js/JSON.parse)
      ;; (js->clj))
   (get-from-localstorage "custom-song-delays"))


(defn set-location-href [url]
  (set! (.-href js/location) url))

(defn set-location-hash [path]
  (set! (.-hash js/location) (str "#" path)))

(rf/reg-event-fx
 ::save-to-localstorage
 (rf/after
  (fn [_ [_ name obj cbevent]]
    (. js/console (log "Saving to localstorage: " name " - " (clj->js obj)))
    (save-to-localstorage name obj)))
 (fn-traced
  [{:keys [db]} [_ name obj callback-event]]
  (merge
   {:db db}
   (if-not (nil? callback-event)
     {:dispatch [callback-event]}
     {}))))

(rf/reg-event-fx
 ::load-from-localstorage
 (fn-traced
  [{:keys [db]} [_ name cb-event]]
  {:db db
   :dispatch [cb-event (get-from-localstorage name)]}))
   

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

(defonce interval-handler                ;; notice the use of defonce
  (let [live-intervals (atom {})]        ;; storage for live intervals
    (fn handler [{:keys [action id frequency event]}]     ;; the effect handler
      (condp = action
        :clean (doseq [interval-id (keys @live-intervals)]                ;; <--- new. clean up all existing 
                (handler {:action :end :id interval-id}))
                 
        :start (swap! live-intervals assoc id (js/setInterval #(rf/dispatch event) frequency))
        :end     (do
                   (js/clearInterval (get @live-intervals id))
                   (swap! live-intervals dissoc id))))))

;; when this code is reloaded `:clean` existing intervals
(interval-handler {:action :clean})

(re-frame.core/reg-fx        ;; the re-frame API for registering effect handlers
 :interval                  ;; the effect id
 interval-handler)
