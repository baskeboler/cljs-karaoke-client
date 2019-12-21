(ns cljs-karaoke.events.user
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events.common :as common-events]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))
(rf/reg-event-fx
 ::create-user
 (fn-traced
  [{:keys [db]} [_ user-name]]
  (let [timestamp (.getTime (js/Date.))
        user-id   (str (random-uuid))
        obj       {:id      user-id
                   :name    user-name
                   :created timestamp}]
    {:db       (-> db
                   (assoc :user obj))
     :dispatch [::common-events/save-to-localstorage "user" obj]})))

(rf/reg-event-fx
 ::init
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::common-events/load-from-localstorage "user" ::handle-load-user-from-localstorage]}))

(defn- keys->keywords [obj]
  (->> (for [[k v] obj] [(keyword k) v])
       (into {})))
(rf/reg-event-fx
 ::handle-load-user-from-localstorage
 (fn-traced
  [{:keys [db]} [_ user-obj]]
  {:db (if-not user-obj
         db
         (-> db
             (assoc :user (keys->keywords user-obj))))
   :dispatch [::init-complete]}))

(rf/reg-event-db
 ::init-complete
 (fn-traced [db _] db))
