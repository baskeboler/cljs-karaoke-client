(ns cljs-karaoke.audio
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [chan <! >! go go-loop] :include-macros true]
            [cljs-karaoke.audio-input]))
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
    (when audio
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
           (fn [evt] (go (>! out-chan (playing evt)))))))
    out-chan))




