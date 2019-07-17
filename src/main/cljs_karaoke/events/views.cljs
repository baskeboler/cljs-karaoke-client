(ns cljs-karaoke.events.views
  ;; {:reader/alias {events cljs-karaoke.events}}
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(def initial-views-state
  {:home {}
   :playback {:options-enabled? false}})

(rf/reg-event-fx
 ::init-views-state
 (fn-traced
  [{:keys [db]} _]
  {:db (-> db
           (assoc :views initial-views-state))
   :dispatch [::views-state-ready]}))

(rf/reg-event-db
 ::views-state-ready
 (fn-traced
  [db _]
  (. js/console (log "views state ready"))
  (-> db
      (assoc :views-state-ready? true))))
(rf/reg-event-db
 ::set-view-property
 (fn-traced [db [_ view-name property-name property-value]]
            (-> db
                (assoc-in [:views view-name property-name] property-value))))
