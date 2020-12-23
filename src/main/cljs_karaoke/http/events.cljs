(ns cljs-karaoke.http.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [ajax.core :as ajax]))


(declare resolve-error-evt-vec)

(rf/reg-event-fx
 ::get
 (fn-traced
  [{:keys [db] :as cofx} [_ arg]]
  (let [{:keys [url on-success on-error response-format]
         :or   {response-format (ajax/json-response-format)
                on-error        ::generic-error-handler}} arg]
    {:http-xhrio {:method          :get
                  :uri             url
                  :timeout         8000
                  :response-format response-format
                  :on-success      [on-success]
                  :on-failure      (resolve-error-evt-vec on-error)}})))

(defn- resolve-error-evt-vec [on-error]
  (cond
    (keyword? on-error) [on-error]
    (vector? on-error) on-error
    :else (do
            (println "[WARNING] I don't know what this error handler is: " on-error)
            on-error)))

(rf/reg-event-fx
 ::generic-error-handler
 (fn-traced
  [{:keys [db]} [_ error]]
  (println "generic error handling: " error)
  {}))
