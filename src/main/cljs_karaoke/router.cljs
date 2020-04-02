(ns cljs-karaoke.router
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.protocols :refer [handle-route]]))
(def routes
  ["/" {""                               :home
        "playlist"                       :playlist
        "editor"                         :editor
        "playback"                       :playback
        ["sing/" [#"[^\/]+" :song-name]] {["/offset/" [#"[\+|-]?\d+" :offset]] :song-with-offset
                                          ""                            :song}}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn create-route-data [handler params]
  {:action handler
   :params params})

;; (defmethod handle-route :songs [_] :playback)
;; (defmethod handle-route :song
  ;; [{:keys [action params]}])
  
;; (defn- route-handler
  ;; ([handler params]
   ;; (condp = handler
     ;; :songs :playback
     ;; :song (do)})
             

(defn dispatch-route
  [{:keys [route-params handler] :as matched-route}]
  (println matched-route)
  (let [route-data (create-route-data handler route-params)
        view (handle-route route-data)]
    (rf/dispatch [:cljs-karaoke.events.views/set-current-view view]))
  #_(when-let [song-name (some-> matched-route :route-params :song-name)]
      (rf/dispatch [::song-events/load-song-if-initialized song-name])
      (if-let [offset (some-> matched-route :route-params :offset)]
        (do
          (rf/dispatch [::events/set-custom-song-delay song-name (js/parseInt offset)])
          (rf/dispatch-sync [::events/set-lyrics-delay (js/parseInt offset)]))
        (rf/dispatch [::events/set-lyrics-delay 0]))))
  ;; (rf/dispatch [:cljs-karaoke.events.views/set-current-view (:handler matched-route)]))
;; (rf/dispatch [::events/set-song-name (get-in matched-route [:route-params :song-name])]))

(defn app-routes []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))
