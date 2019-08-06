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

(defn generic-tween
  ([svg from to element-id ms delay-ms yoyo?]
   (let [intpl (interpolate/interpolate from to)
         times (concat (range 0 1 0.01) [1])
         values (-> intpl
                    (ease/wrap (ease/ease :cubic))
                    (map times))
         element (. svg (querySelector (str "#" element-id)))
         cancel (atom false)
         cancel-chan (async/chan)]
     (go
       (<! cancel-chan)
       (reset! cancel true))
     (go-loop [the-values (if yoyo? (ping-pong values) values)
               d delay-ms]
       (<! (async/timeout d))
       (let [obj-keys (keys (first the-values))]
         (doseq [k obj-keys]
           (. element (setAttribute (str k) (get (first the-values) k ""))))
         (<! (async/timeout 50))
         (when-not  @cancel
           (recur (rest the-values) 0))))
     cancel-chan))
  ([svg from to element-id]
   (generic-tween svg from to element-id 0 true)))

(defn- ping-pong [values]
  (cycle (concat values (reverse values))))

(defn transition-fn [from to opts
                     update-fn delay yoyo?]
  (let [duration (get opts :duration 500)
        times (concat
               (range 0 1 (/ 50 duration))
               [1])
        intpl (interpolate/interpolate from to)
        values (-> intpl
                   (ease/wrap (ease/ease :cubic-in-out))
                   (map times))]
    (update-fn from)
    (go-loop [from from
              to to
              values values
              d delay]
      (when (> d 0)
        (<! (async/timeout d)))
      (let [v         (first values)
            closed?   (nil? v)
            finished? (and closed? (not yoyo?))]
        (when-not closed?
          (update-fn v))
        (when-not finished?
          (<! (async/timeout 50))
          (recur (if closed? to from)
                 (if closed? from to)
                 (if closed?
                   (-> (interpolate/interpolate to from)
                       (ease/wrap (ease/ease :cubic-in-out))
                       (map times))
                   (rest values))
                 (if closed? delay 0)))))))
(defn perform-animation [svg]
  (let [the-chars (get-logo-chars svg)
        from      {:opacity 0.5
                   :scale   1.2}
        to        {:opacity 1.0
                   :scale   1.0}
        duration 1000
        logo-rot-chan (transition/transition -30
                                             30
                                             {:duration 500})]
    (transition-fn 0.2 1.0 {:duration 3000
                            :easing :cubic}
                   (fn [s]
                     (set-scale (get-logo-path svg) s))
                   1000
                   false)
    (transition-fn {:stroke '(0 0 0)
                    :stroke-width 0.1}
                   {:stroke '(255 255 255)
                    :stroke-width 12.0}
                   {:duration 3000}
                   (fn [{:keys [stroke stroke-width]}]
                     (let [l (get-logo-path svg)
                           s (.-style l)
                           [r g b] stroke]
                       (set! (.-stroke s) (str "rgb(" (clojure.string/join "," stroke) ")"))
                       (set! (.-strokeWidth s) stroke-width)))
                       ;; (. l (setAttribute "stroke" (str "rgb(" (clojure.string/join "," stroke) ")")))
                       ;; (. l (setAttribute "stroke-width" stroke-width))))
                   4000
                   false)
                   
    (doseq [[i c] (map vector (range) the-chars)]
      (transition-fn from to {:duration duration}
                     (fn [{:keys [opacity scale]}]
                       (-> c
                           (set-opacity opacity)
                           (set-scale scale)))
                     (* i 500)
                     false))))

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

