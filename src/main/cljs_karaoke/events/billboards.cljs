(ns cljs-karaoke.events.billboards
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-event-fx
 ::display-billboard
 (fn-traced
  [{:keys [db]} [_ b duration]]
  {:db             (-> db
                       (update :billboards conj b))
   :dispatch-later [{:dispatch [::remove-billboard b]
                     :ms       duration}]}))

(rf/reg-event-fx
 ::remove-billboard
 (fn-traced
  [{:keys [db]} [_ b]]
  {:db (-> db
           (update :billboards
                   (fn [billboards]
                     (mapv (fn [billboard]
                             (if (= (:id billboard) (:id b))
                               (-> billboard
                                   (assoc :visible? false))
                               billboard))
                           billboards))))
   :dispatch-later [{:ms 1000
                     :dispatch [::delete-billboard b]}]}))

(rf/reg-event-db
 ::delete-billboard
 (fn-traced
  [db [_ b]]
  (-> db
      (update :billboards
              (fn [billboards]
                (filterv #(not= (:id %) (:id b)) billboards))))))

(defn song-name-billboard-event [song-name]
  [::display-billboard
   {:type :song-name-display
    :text song-name}
   5000])
