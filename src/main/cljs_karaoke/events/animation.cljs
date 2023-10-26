(ns cljs-karaoke.events.animation
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-event-fx
 ::time-tick
 (fn-traced
  [{:keys [db]} _]
  (let [now (js/Date.now)]
    {:db (-> db
             (assoc-in [:animation :timer] now))
     :dispatch-later [{:ms 50 :dispatch [::time-tick]}]})))

(rf/reg-sub
 ::timer
 (fn-traced
  [db _]
  (get-in db [:animation :timer] 0)))

(rf/reg-sub
 ::tween
 :<- [::timer]
 (fn
  [timer [_ tween-fn]]
  (assert (fn? tween-fn))
  (tween-fn timer)))
