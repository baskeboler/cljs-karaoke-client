(ns cljs-karaoke.subs.audio
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::audio-data
 (fn [db _]
   (:audio-data db)))

(defn reg-audio-data-sub [sub-name attr-name]
  (rf/reg-sub
   sub-name
   :<- [::audio-data]
   (fn [data _]
     (get data attr-name))))

(reg-audio-data-sub ::feedback-reduction? :feedback-reduction?)
(reg-audio-data-sub ::reverb-buffer :reverb-buffer)
(reg-audio-data-sub ::dry-gain :dry-gain)
(reg-audio-data-sub ::wet-gain :wet-gain)
(reg-audio-data-sub ::effect-input :effect-input)
(reg-audio-data-sub ::output-mix :output-mix)
(reg-audio-data-sub ::audio-input :audio-input)
(reg-audio-data-sub ::lp-input-filter :lp-input-filter)
(reg-audio-data-sub ::clean-analyser :clean-analyser)
(reg-audio-data-sub ::reverb-analyser :reverb-analyser)
(reg-audio-data-sub ::freq-data :freq-data)
(reg-audio-data-sub ::audio-context :audio-context)
(reg-audio-data-sub ::stream :stream)
(reg-audio-data-sub ::audio-input-available? :audio-input-available?)
(reg-audio-data-sub ::recording-enabled? :recording-enabled?)
(reg-audio-data-sub ::recorded-blobs :recorded-blobs)
(reg-audio-data-sub ::media-recorder :media-recorder)
(reg-audio-data-sub ::recording? :recording?)

(rf/reg-sub
 ::recording-button-enabled?
 :<- [::recording?]
 :<- [::recording-enabled?]
 (fn [[recording? enabled?] _]
   (and enabled? (not recording?))))

(rf/reg-sub
 ::microphone-enabled?
 :<- [::output-mix]
 (fn [mix _]
   (not (nil? mix))))

(rf/reg-sub
 ::song-stream
 (fn [db _]
    (:song-stream db)))
