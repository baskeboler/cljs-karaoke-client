(ns cljs-karaoke.events.modals
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))
(rf/reg-event-fx
 ::modal-push
 (fn-traced
  [{:keys [db]} [_ modal]]
  {:db (-> db
           (update :modals conj modal))
   :dispatch [::modal-activate]}))

(rf/reg-event-db
 ::modal-activate
 (fn-traced
  [db _] db))

(rf/reg-event-db
 ::modal-pop
 (fn-traced
  [db _]
  (-> db
      (update :modals pop))))
