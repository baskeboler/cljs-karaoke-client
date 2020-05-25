(ns cljs-karaoke.router.core
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            ;; [re-frame.core :as rf]
            ;; [cljs-karaoke.events :as events]
            ;; [cljs-karaoke.events.songs :as song-events]
            ;; [cljs-karaoke.key-bindings :as k]
            [cljs-karaoke.protocols :refer [handle-route dispatch-view]]))

(def routes
  ["/" {""                               :home
        "playlist"                       :playlist
        "editor"                         :editor
        "playback"                       :playback
        "random-song"                    :random-song
        ["sing/" [#"[^\/]+" :song-name]] {["/offset/" [#"[\+|-]?\d+" :offset]] :song-with-offset
                                          ""                                   :song}}])

(defn- parse-url [url]
  (bidi/match-route routes url))


(defn create-route-data [handler params]
  {:action handler
   :params params})


(defn dispatch-route
  [{:keys [route-params handler] :as matched-route}]
  (println matched-route)
  (let [route-data (create-route-data handler route-params)
        view (handle-route route-data)]
    (dispatch-view view)))

(defonce ^:export router (pushy/pushy dispatch-route parse-url))


(defn app-routes []
  (pushy/start! router))

(def url-for (partial bidi/path-for routes))

;; (defmethod handle-route :default [arg])
  
