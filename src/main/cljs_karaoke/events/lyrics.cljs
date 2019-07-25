(ns cljs-karaoke.events.lyrics
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [day8.re-frame.async-flow-fx]
            [ajax.core :as ajax]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.common :as common-events]
            [cljs.tools.reader.edn :as reader]))

(defn load-lyrics-flow []
  {:rules [{:when :seen-any-of?
            :events [::handle-fetch-lyrics-success
                     ::handle-fetch-lyrics-failure]
            :dispatch [::fetch-lyrics-complete]
            :halt? true}]})

(rf/reg-event-fx
 ::fetch-lyrics
 (fn-traced
  [{:keys [db]} [_ name process]]
  (. js/console (log "Fetching lyrics for " name))
  {:db (-> db
           (assoc :lyrics-loaded? false)
           (assoc :lyrics-fetching? true))
   :http-xhrio {:method :get
                :uri (str events/base-storage-url "/lyrics/" name ".edn")
                :timeout 8000
                :response-format (ajax/text-response-format)
                :on-success [::handle-fetch-lyrics-success]
                :on-failure [::handle-fetch-lyrics-failure]}
   :async-flow (load-lyrics-flow)}))

(rf/reg-event-fx
 ::handle-fetch-lyrics-success
 (fn-traced
  [{:keys [db]} [_ response]]
  (let [lyrics (-> response
                   (reader/read-string))
        new-db (-> db
                   (assoc :lyrics lyrics)
                   (assoc :lyrics-fetching? false)
                   (assoc :lyrics-loaded? true))]
     {:db new-db})))

(rf/reg-event-fx
 ::handle-fetch-lyrics-failure
 (fn-traced
  [{:keys [db]} [_ error]]
  (. js/console (log "Failed to load lyrics " error))
  {:db db}))

(rf/reg-event-fx
 ::fetch-lyrics-complete
 (fn-traced
  [{:keys [db]} _]
  (. js/console (log "Fetch lyrics flow complete"))
  {:db db}))
