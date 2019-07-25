(ns cljs-karaoke.subs.http-relay
  (:require [re-frame.core :as rf :include-macros true]))

(rf/reg-sub
 ::http-relay-listener-id
 (fn [db _]
   (:relay-listener-id db)))

(rf/reg-sub
 ::remote-control-id
 (fn [db _]
   (:remote-control-id db)))
