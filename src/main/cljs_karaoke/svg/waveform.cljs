(ns cljs-karaoke.svg.waveform
  (:require [goog.math :as gmath]
            [thi.ng.geom.bezier :as beziers]
            [thi.ng.geom.core :as geom]
            [thi.ng.geom.vector :as geom-vec :include-macros true :refer [vec2 Vec2]]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.geom.path :as paths]
            [thi.ng.geom.types :as geom-types]
            [cljs.core.async :as async :refer [go go-loop chan <! >!]]
            [re-frame.core  :as rf]
            [cljs-karaoke.events.audio :as aevents]
            [cljs-karaoke.subs.audio :as asubs]))
(defn median [values]
  (cond
    (empty? values) (throw (js/Error "Empty value array"))
    (= 1 (count values)) (first values)
    (odd? (count values)) (-> (sort values)
                              (nth (js/Math.floor (divide (count values) 2))))
    :else (let [v1 (-> (sort values)
                       (nth (js/Math.floor (divide (count values) 2))))
                v2 (-> (sort values)
                       (nth (dec (js/Math.floor (divide (count values) 2)))))]
            (/ (+ v1 v2) 2))))

(defn ^export read-audio-buffer [^js/AudioBuffer audio-buffer width]
  (let [left-channel (. audio-buffer (getChannelData 0))
        data-len (.-length left-channel)
        sample-size (int (divide data-len width))
        parts (partition sample-size left-channel)]
    (map median parts)))

(comment
  (let [c            @(rf/subscribe [::asubs/audio-context])
        song-stream  @(rf/subscribe [::asubs/song-stream])
        input-stream @(rf/subscribe [::asubs/stream])
        anal         @(rf/subscribe [::asubs/clean-analysesr])
        buf          (js/Float32Array. (. anal -fftSize))]
    (. anal (getFloatTimeDomainData buf))
    (. js/console (log c song-stream input-stream anal buf))
    (map-indexed (fn [i v] [i (* 100 v)]) buf)))

(defn audio-wave-display [buffer1 buffer2 widget-width]
  (let [points (map vector (range) buffer1)]
    (svg/svg (svg/svg-attribs {:width widget-width} {})
             (->> (beziers/auto-spline2 points)
                  (into [])
                  (paths/path2)
                  #(svg/as-svg % {}))))) 

(defn wave-component []
  (let [anal (rf/subscribe [::asubs/clean-analyser])
        buf (js/Float32Array. (. anal -fftSize))]
    (. anal (getFloatTimeDomainData buf))))
    

;; (def curve-points (list (vec2 100 0) (vec2 200 100) (vec2 100 0)))

;; (def curve (beziers/auto-spline2 curve-points true))
(defn get-audio-buffer [src-url]
 (let [ctx (js/OfflineAudioContext. 2 (* 44100 60 5) 44100)
       a-ctx (js/AudioContext.)
       src-buf  (.. ctx (createBufferSource))
       src-url (.. (.. js/document (querySelector "#main-audio")) -src)
       out (chan)
       handle-rendered (fn [rendered-buffer]
                         (let [data (.getChannelData rendered-buffer 0)]
                           (js/console.log "rendering completed successfully: " data) 
                           (go
                             (>! out (-> data
                                         aclone
                                         js->clj)))))
       handle-decoded (fn [buf]
                        (println "hello!")
                        (set! (-> src-buf .-buffer) buf)
                        (-> src-buf (.connect (-> ctx .-destination)))
                        (-> src-buf (.start))
                        (-> ctx
                            (.startRendering)
                            (.then handle-rendered)))
       handle-fetch-response (fn [resp]
                              (println "Got response")
                              (let [r-buf (-> resp (.arrayBuffer))]
                                (println "got buffer: " r-buf)
                                (-> r-buf
                                    (.then
                                     (fn [rr-buf] 
                                       (-> a-ctx
                                           (.decodeAudioData
                                            rr-buf
                                            handle-decoded)))))))]
   (println "fetching: " src-url)
   (-> (js/fetch src-url)
       (.then handle-fetch-response))
   out))
