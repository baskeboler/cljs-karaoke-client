(ns cljs-karaoke.events.song-info
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [cljs.reader :as reader]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-karaoke.http.events :as http-events]))

(rf/reg-event-fx
 ::fetch-song-metadata
 (rf/after
  (fn [_ _]
    (println "fetching song metadata database")))
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::http-events/get
              {:url "/data/song-info.edn"
               :response-format (ajax/text-response-format)
               :on-success ::handle-fetch-song-metadata-success
               :on-error ::fetch-song-metadata-complete}]}))

(rf/reg-event-fx
 ::handle-fetch-song-metadata-success
 (rf/after
  (fn [_ _] (println "fetched song metadata database successfully")))
 (fn-traced
  [{:keys [db]} [_ response]]
  {:db (-> db
           (assoc :song-metadata (reader/read-string response)))
   :dispatch [::fetch-song-metadata-complete]}))

(rf/reg-event-db
 ::fetch-song-metadata-complete
 (fn-traced
  [db _]
  (println "song metadata fetch complete")
  db))

