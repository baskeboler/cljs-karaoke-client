(ns cljs-karaoke.animation
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf]
            [bardo.transition :as transition]
            [bardo.ease :as ease]
            [bardo.interpolate :as interpolate]
            [stylefy.core :as stylefy]
            [cljs.core.async :as async :refer [<! >! go go-loop]]
            [goog.events :as gevents]
            [goog.dom :as dom]))

(deftype ColorStringValue [value])
(deftype ColorRGBValue [r g b])
(deftype StringValue [value])

(defprotocol AttrValue
  (attr->str [this]))

(extend-protocol AttrValue
  ColorStringValue
  (attr->str [this] (.-value this))
  ColorRGBValue
  (attr->str [this] (str "rgb(" (.-r this) ", " (.-g this) ", " (.-b this) ")"))
  StringValue
  (attr->str [this] (.-value this)))
(def loader-logo
  {:display :block
   :position :absolute
   :height "60vh"
   :width "60vw"
   :top "50%"
   :left "50%"
   :transform "translate(-50%, -50%)"})

(defn value-str [v]
  (if-not (string? v)
    (str "rgb(" (clojure.string/join "," v) ")")
    v))
(defn attribute-setter
  ([attr-name value]
   (fn [obj]
     (. obj (setAttribute attr-name (value-str value)))
     obj))
  ([attr-name]
   (fn [obj value]
     (. obj (setAttribute attr-name (value-str value)))
     obj)))

(def stroke-setter (attribute-setter "stroke"))
(def trasform-setter (attribute-setter "transform"))
(def fill-color-setter (attribute-setter "color"))

(defn- set-opacity [obj opacity]
  (. obj (setAttribute "opacity" opacity))
  obj)
(defn- set-scale [obj scale]
  (. obj (setAttribute "transform-origin" "50% 50%"))
  (. obj (setAttribute "transform" (str "scale(" scale ", " scale ")")))
  obj)

(defn get-logo-chars [svg]
  (->> (for [c (mapv #(str "char" %) (range 1 8))]
         (. svg (querySelector (str "#" c))))
       (into [])))
(defn- get-logo-path [svg]
  (. svg (querySelector "#logo")))

(defn- ping-pong [values]
  (cycle (concat values (reverse values))))
(defn perform-animation [svg]
  (let [the-chars (get-logo-chars svg)
        intpl (interpolate/interpolate
               {:opacity 0.5
                :scale   1.2
                :fillColor '(255 255 255)
                :stroke '(0 0 0)
                :logo-rotation -30}
                
               {:opacity 1.0
                :scale   1.0
                :fillColor '(0 0 0)
                :stroke '(255 255 255)
                :logo-rotation 30})
        times (concat (range 0.0 1.0 0.01) [1])
        values (-> intpl
                   (ease/wrap (ease/ease :cubic))
                   (map times))]
    (go-loop [the-values (ping-pong values)]
      (let [{:keys [opacity scale fillColor stroke logo-rotation] :as v} (first the-values)]
        ;; (println opacity " - " scale)
        ;; (. c1 (setAttribute "opacity" opacity))
        (doseq [c the-chars]
          (-> c
              (set-opacity opacity)
              (set-scale scale)
              (stroke-setter stroke)
              (fill-color-setter fillColor)))
        
        (. (get-logo-path svg) (setAttribute "transform" (str "rotate(" logo-rotation ")")))
        (<! (async/timeout 25))
        (recur (rest the-values))))))

    
(defn logo-animation []
  (let [l (reagent/atom nil)
        element-id (str (random-uuid))]
    (reagent/create-class
     {:component-will-unmount
      (fn [this]
        (gevents/unlistenByKey (.. this -state -listener-key))
        (. this (setState nil))
        (println "unlistened event"))
      :initial-state {:listener-key nil}
      :component-did-mount
      (fn [this]
        (let [s (dom/getElement  element-id)
              l2 (gevents/listen ^js s ^js (. gevents/EventType -LOAD)
                                 (fn []
                                   (let [svg (.-contentDocument s)
                                         txt (. svg (querySelector "#logo-text"))
                                         logo (. svg (querySelector "#logo"))]
                                     ;; (. txt (setAttribute "opacity" 0))
                                     (set! (.. txt -style -transformOrigin) "0.5 0.5")
                                     (. (get-logo-path svg) (setAttribute "transform-origin" "50% 50%"))
                                     (perform-animation svg)
                                     (js/console.log "Loaded doc " txt))))]

          ;; (reset! l l2)
          (. this (setState (clj->js {:listener-key l2})))
          (println "Got element " s)))
      
      :render (fn [this]
                [:div.my-inlined-svg
                 [:object
                  (stylefy/use-style
                   loader-logo
                   {:id element-id
                    :data "images/logo-2.svg"
                    :type "image/svg+xml"})]])})))

