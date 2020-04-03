(ns cljs-karaoke.views.dispatcher
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events.views :as view-events]
            [cljs-karaoke.protocols :refer [dispatch-view ViewDispatcher]]))

(println "Loading view dispatcher")

(extend-protocol ViewDispatcher
  cljs.core/Keyword
  (dispatch-view [this]
    (rf/dispatch [::view-events/set-current-view this])))
