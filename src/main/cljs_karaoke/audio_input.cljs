(ns cljs-karaoke.audio-input
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]))

(def deferred-inits (atom []))

(defn init-pending-fns []
  (println "Executing pending inits")
  (loop [f (first @deferred-inits)]
    (when-not (nil? f)
      (reset! deferred-inits (into [] (rest @deferred-inits)))
      (f)
      (recur (first @deferred-inits)))))

(def audio-context (atom nil))

(defn init-audio-context []
  (reset! audio-context (js/AudioContext.)))

(defn convert-to-mono [input]
  (let [splitter (. @audio-context (createChannelSplitter 2))
        merger (. @audio-context (createChannelMerger 2))]
    (. input (connect splitter))
    (. splitter (connect merger 0 0))
    (. splitter (connect merger 0 1))
    merger))

(def feedback-reduction? true)
(def reverb-buffer (atom nil))
(def dry-gain (atom nil))
(def wet-gain (atom nil))
(def effect-input (atom nil))
(def output-mix (atom nil))
(def audio-input-atom (atom nil))
(def lp-input-filter (atom nil))
(def clean-analyser (atom nil))
(def reverb-analyser (atom nil))

(def constraints
  {:audio {:optional [{:echoCancellation false}]}})

(defn create-lp-input-filter []
  (let [f (.createBiquadFilter @audio-context)]
    (set! (.-value (.-frequency f)) 2048)
    (reset! lp-input-filter f)
    f))
(defn cross-fade [v]
  (let [gain1 (js/Math.cos (* v 0.5 js/Math.PI))
        gain2 (js/Math.cos (* (- 1.0 v) 0.5 js/Math.PI))]
    (set! (.-value (.-gain @dry-gain)) gain1)
    (set! (.-value (.-gain @wet-gain)) gain2)))
(defn init-reverb-buffer []
  (let [req (js/XMLHttpRequest.)]
    (.open req "GET" "/media/cardiod-rear-levelled.wav" true)
    (set! (.-responseType req) "arraybuffer")
    (set! (.-onload req)
          (fn []
            (.decodeAudioData @audio-context
                              (.-response req)
                              (fn [buffer]
                                (reset! reverb-buffer buffer)
                                (println "reverb buffer set")
                                (init-pending-fns)))))
    (.send req)))


(defn create-reverb []
  (let [convolver (.createConvolver @audio-context)]
    (set! (.-buffer convolver) @reverb-buffer)
    (. convolver (connect @wet-gain))
    convolver))

(defn on-stream [stream]
  (let [input (.createMediaStreamSource @audio-context stream)
        audio-input (if feedback-reduction?
                      (do
                        (let [i (convert-to-mono input)]
                          (. i (connect (create-lp-input-filter)))
                          @lp-input-filter))
                      (covert-to-mono input))
        audio-filter (.createBiquadFilter @audio-context)
        analyser1 (.createAnalyser @audio-context)
        analyser2 (.createAnalyser @audio-context)
        output-mix1 (.createGain @audio-context)
        dry-gain1 (.createGain @audio-context)
        wet-gain1 (.createGain @audio-context)
        effect-input1 (.createGain @audio-context)]
    (. audio-input (connect dry-gain1))
    (. audio-input (connect wet-gain1))
    (. audio-input (connect effect-input1))
    (. audio-input (connect analyser1))
    (. dry-gain1 (connect output-mix1))
    (. wet-gain1 (connect output-mix1))

    ;; (when feedback-reduction?
      ;; (. audio-input (connect (create-lp-input-filter)))
    ;; (set! audio-input @lp-input-filter))
    (. output-mix1 (connect analyser2))
    (. output-mix1 (connect (.-destination @audio-context)))
    (reset! audio-input-atom audio-input)
    (reset! dry-gain dry-gain1)
    (reset! wet-gain wet-gain1)
    (reset! effect-input effect-input1)
    (reset! output-mix output-mix1)
    (reset! clean-analyser analyser1)
    (reset! reverb-analyser analyser2)

    (cross-fade 1.0)
    (create-reverb)
    ;; (set! (.-value (.-frequency audio-filter)) 60.0)
    ;; (set! (.-type audio-filter) "notch")
    ;; (set! (.-Q audio-filter) 10.0)
    ;; (.connect input audio-filter)
    ;; (.connect audio-filter analyser)
    ;; (reset! analyser analyser)
    ;; audio-filter))
    (println "Audio input init complete.")))

(defn on-stream-error [e]
  (println e))

(defn get-microphone-input []
  (let [args (clj->js
               constraints)]
    (-> js/navigator
        (.getUserMedia args on-stream on-stream-error))))
    

(defn ^export init-audio-input []
  (init-audio-context)
  (let [f (fn []
            (get-microphone-input))]
    (swap! deferred-inits conj f)
    (init-reverb-buffer)))
