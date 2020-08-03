(ns cljs-karaoke.mobile
  (:require [mount.core :refer [defstate]]
            [reagent.core]
            [re-frame.core :as rf]
            ["shake.js" :as Shake]
            [clojure.string :as str]
            [cljs-karaoke.views.toasty :refer [trigger-toasty]]
            [cljs-karaoke.events.audio :as audio-events]
            [goog.labs.userAgent.device :as device])) 


(defn- ios? []
  (-> (. js/navigator -platform)
      (str/lower-case)
      (str/includes? "ios")))

(defn- mobile-device? []
  (device/isMobile))

(defonce shake (Shake. (clj->js {:threshold 15 :timeout 1000})))

(.start shake)

(defonce my-shake-event (Shake. (clj->js {:threshold 15 :timeout 1000})))

(defn on-shake [evt] (trigger-toasty))

(def mobile? (mobile-device?))


(defn- init []
  (println "starting mobile")
  (if mobile?
    (do
      (println "mobile device, ignoring keybindings")
      (. js/window (addEventListener "shake" on-shake false))
      (when (ios?)
        (rf/dispatch-sync [::audio-events/set-audio-input-available? false])
        (rf/dispatch-sync [::audio-events/set-recording-enabled? false])))))
  ;; (init-keybindings!)))

(defstate mobile
  :start (init))
