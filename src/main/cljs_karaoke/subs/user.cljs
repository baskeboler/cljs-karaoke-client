(ns cljs-karaoke.subs.user
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events.common :as common-events]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))

(rf/reg-sub
 ::user
 (fn-traced
  [db _]
  (:user db)))

(rf/reg-sub
 ::user-ready?
 :<- [::user]
 (fn-traced
  [user _]
  (some? user)))
