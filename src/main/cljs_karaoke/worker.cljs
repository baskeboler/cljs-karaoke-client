(ns cljs-karaoke.worker
  (:require [cognitect.transit :as t]
            [cljs-karaoke.worker.impl :as impl]))
(enable-console-print!)
(defonce id (random-uuid))

(defonce reader (t/reader :json))

(defn- on-message
  [event]
  (when (nil? (.-source event))
    (let [message (t/read reader (.-data event))])))
      ;; ())))

(defn ^:export init! []
  (js/console.log "worker!")

  (.. js/self
      (addEventListener
       "install"
       (fn [event]
         (js/console.log "installing")
         (.. event
             (waitUntil (.. js/caches
                            (open "v1")
                            (then
                             (fn [cache]
                               (.. cache
                                   (addAll
                                    #js ["/index.html"
                                         "/js/main.js"
                                         "/js/shared.js"]))))))))))
      
  (.. js/self
      (addEventListener
       "message"
       (fn [event]
         (js/console.log "i got a message! " event)))))
