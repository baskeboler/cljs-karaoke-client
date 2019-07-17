(ns cljs-karaoke.audio
  (:require [re-frame.core :as rf]
            [cljs.core.async :as async :refer [chan <! >! go go-loop] :include-macros true]))

(defmulti process-audio-event (fn [event] (:type event)))
(defmethod process-audio-event :default [event]
  (println "event ignored: " event))

(defn canplaythrough [evt] {:type :canplaythrough :event evt})
(defn canplay [evt] {:type :canplay :event evt})
(defn ended [evt] {:type :ended :event evt})
(defn play [evt] {:type :play :event evt})
(defn pause [evt] {:type :pause :event evt})
(defn timeupdate [evt] {:type :timeupdate :event evt})
(defn playing [evt] {:type :playing :event evt})

(defn setup-audio-listeners [audio]
  (let [out-chan (chan)]
    (.. audio
        (addEventListener
         "canplay"
         (fn [evt] (go (>! out-chan (canplay evt))))))
    (.. audio
        (addEventListener
         "canplaythrough"
         (fn [evt] (go (>! out-chan (canplaythrough evt))))))
    (.. audio
        (addEventListener
         "ended"
         (fn [evt] (go (>! out-chan (ended evt))))))
    (.. audio
        (addEventListener
         "play"
         (fn [evt] (go (>! out-chan (play evt))))))
    (.. audio
        (addEventListener
         "pause"
         (fn [evt] (go (>! out-chan (pause evt))))))
    (.. audio
        (addEventListener
         "timeupdate"
         (fn [evt] (go (>! out-chan (timeupdate evt))))))
    (.. audio
        (addEventListener
         "playing"
         (fn [evt] (go (>! out-chan (playing evt))))))
    out-chan))

;; (def audio-context (js/AudioContext.))
;; (def analyser (atom nil))

;; (defn on-stream [stream]
;;   (let [input (.createMediaStreamSource audio-context stream)
;;         audio-filter (.createBiquadFilter audio-context)
;;         analyser (.createAnalyser audio-context)]
;;     (set! (.-value (.-frequency audio-filter)) 60.0)
;;     (set! (.-type audio-filter) "notch")
;;     (set! (.-Q audio-filter) 10.0)
;;     (.connect input audio-filter)
;;     (.connect audio-filter analyser)
;;     (reset! cljs-karaoke.audio/analyser analyser)
;;     audio-filter))

;; (defn on-stream-error [e]
;;   (println e))

;; (defn get-microphone-input []
;;   (let [args (clj->js
;;               {:audio true})]
;;     (-> js/navigator
;;         (.getUserMedia args on-stream on-stream-error))))
    
