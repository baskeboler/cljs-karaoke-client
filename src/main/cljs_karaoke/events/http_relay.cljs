(ns cljs-karaoke.events.http-relay
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [ajax.core :as ajax]
            [cljs.reader :as reader]
            [cljs-karaoke.remote-control.commands :as cmds]
            [cljs-karaoke.remote-control.queue :as remote-queue]))
(def base-http-relay-url "https://demo.httprelay.io/link/")

(defn ^export generate-remote-control-id []
  (->> (random-uuid)
       str
       (take 8)
       (apply str)))

(rf/reg-event-fx
 ::init-http-relay-listener
 (fn-traced
  [{:keys [db]} _]
  (let [relay-id (generate-remote-control-id)]
    {:db (-> db
             (assoc :relay-listener-id relay-id))
     :dispatch [::listen-http-relay relay-id]})))

(rf/reg-event-fx
 ::listen-http-relay
 (fn-traced
  [{:keys [db]} [_ relay-id]]
  {:db db
   :http-xhrio {:method :get
                :uri (str base-http-relay-url "karaoke-" relay-id)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [::handle-http-relay-response]
                :on-failure [::handle-http-relay-failure]}}))

(defn process-remote-command [db cmd]
  (. js/console (log "Got remote command " cmd))
  {:db db
   :dispatch-n [[::listen-http-relay (:relay-listener-id db)]]})

(rf/reg-event-fx
 ::handle-http-relay-response
 (rf/after
  (fn [db [_ cmd]]
    (remote-queue/queue-remote-command cmd)))
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
                :uri (str base-http-relay-url "karaoke-" (:remote-control-id db))
                :body (-> command clj->js js/JSON.stringify)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success ::handle-http-relay-response
                :on-failure ::handle-http-relay-failure}}))
