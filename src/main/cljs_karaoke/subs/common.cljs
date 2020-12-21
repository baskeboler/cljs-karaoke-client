(ns cljs-karaoke.subs.common
  (:require [re-frame.core :as rf]))

(defn ^:export reg-attr-sub
  [name key]
  (rf/reg-sub
   name
   (fn [db _]
     (get db key))))
