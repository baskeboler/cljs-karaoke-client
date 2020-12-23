(ns cljs-karaoke.events.metrics
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-karaoke.events.common :as common-events]))

(rf/reg-event-fx
 ::load-user-metrics-from-localstorage
 (fn-traced
  [{:keys [db]} _]
  {:dispatch [::common-events/load-from-localstorage "user-metrics" ::handle-load-metrics-from-localstorage]}))

(rf/reg-event-fx
 ::handle-load-metrics-from-localstorage
 (fn-traced
  [{:keys [db]} [_ obj]]
  {:db       (-> db
                 (update-in  [:metrics]
                             #(merge-with
                               (fn [v1 v2]
                                 (merge-with + v1 v2))
                               (if-not (nil? obj)
                                 (into {} (for [[k v] obj] [(keyword k) v]))
                                 {})
                               %)))
   :dispatch [::load-metrics-from-localstorage-complete]}))
(rf/reg-event-fx
 ::save-user-metrics-to-localstorage
 (fn-traced
  [{:keys [db]} _]
  {:dispatch [::common-events/save-to-localstorage "user-metrics" (:metrics db) ::save-user-metrics-to-localstorage-complete]}))

(common-events/reg-identity-event ::load-metrics-from-localstorage-complete)
(common-events/reg-identity-event ::save-user-metrics-to-localstorage-complete)

(rf/reg-event-fx
 ::inc-song-play-count
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db (-> db
           (update-in [:metrics :song-plays song-name] inc))
   :dispatch [::save-user-metrics-to-localstorage]}))
