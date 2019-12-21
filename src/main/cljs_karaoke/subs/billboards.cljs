(ns cljs-karaoke.subs.billboards
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]))
(rf/reg-sub
 ::billboards
 (fn-traced
  [db _]
  (:billboards db)))
