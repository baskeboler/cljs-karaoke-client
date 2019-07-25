(ns cljs-karaoke.events.http-relay
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [ajax.core :as ajax]
            [cljs.reader :as reader]))
(def base-http-relay-url "https://httprelay.io/link/")

(rf/reg-event-fx
 ::init-http-relay-listener
 (fn-traced
  [db _]
  (let [relay-id (random-uuid)]
    {:db (-> db
             (assoc :relay-listener-id relay-id))
     :dispatch [::listen-http-relay relay-id]})))

(rf/reg-event-fx
 ::listen-http-relay
 (fn-traced
  [{:keys [db]} [_ relay-id]]
  {:db db
   :http-xhrio {:method :get
                :uri (str base-http-relay-url relay-id)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::handle-http-relay-response]
                :on-failure [::handle-http-relay-failure]}}))

(defn process-remote-command [db cmd]
  (. js/console (log "Got remote command " cmd))
  {:db db
   :dispatch-n [[::listen-http-relay (:relay-listener-id db)]]})

(rf/reg-event-fx
 ::handle-http-relay-response
 (fn-traced
  [{:keys [db]} [_ response]]
  (. js/console (log "got response " response))
  (process-remote-command db response)))

(rf/reg-event-fx
 ::handle-http-relay-failure
 (fn-traced
  [{:keys [db]} [_ err]]
  (. js/console (log "http relay listener error " err))
  {:db db
   :dispatch [::listen-http-relay (:relay-listener-id db)]}))


(defn play-song-command [song]
  {:command :play-song
   :song song})

(defn stop-command []
  {:command :stop})

(defn playlist-next-command []
  {:command :playlist-next})

(defmulti execute-command
  "execute remote control commands"
  (fn [cmd] (:command cmd)))

(defmethod execute-command :default
  [cmd]
  (println "Unknown remote control command: " cmd))

(rf/reg-event-db
 ::set-remote-control-id
 (fn-traced
  [db [_ remote-id]]
  (-> db (assoc :remote-control-id remote-id))))

(rf/reg-event-fx
 ::remote-control-command
 (fn-traced
  [{:keys [db]} [_ command]]
  {:db db
   :http-xhrio {:method :post
                :uri (str base-http-relay-url (:remote-control-id db))
                :body (-> command clj->js js/JSON.stringify)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success ::handle-http-relay-response
                :on-failure ::handle-http-relay-failure}}))
