(ns cljs-karaoke.events.notifications
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))
(rf/reg-event-fx
 ::add-notification
 (fn-traced
  [{:keys [db]} [_ n]]
  {:db (-> db (update :notifications conj n))
   :dispatch-later [{:ms 5000
                     :dispatch [::dismiss-notification (:id n)]}]}))

(rf/reg-event-db
 ::dismiss-notification
 (fn-traced
  [db [_ notification-id]]
  (-> db
      (update :notifications (fn [notis]
                               (filterv
                                #(not= notification-id (:id %))
                                notis))))))
