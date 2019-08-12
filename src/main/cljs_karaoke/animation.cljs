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

(defrecord TranslateValue [x y])
(defrecord RotateValue [x y z])
(defrecord ScaleValue [x y])

(defrecord TransformValue [translate rotate scale])

(defn ^TranslateValue translate-value
  [x y] (->TranslateValue x y))

(defn ^RotateValue rotate-value
  ([x y z] (->RotateValue x y z))
  ([x y] (rotate-value x y 0))
  ([x] (rotate-value 0 0 x)))

(defn ^ScaleValue scale-value
  ([x y] (->ScaleValue x y))
  ([x] (scale-value x x)))

(defn ^TransformValue transform-value
  [{scale :scale
    translate :translate
    rotate :rotate
    :or {scale (scale-value 1)
         rotate (rotate-value 0)
         translate (translate-value 0 0)}}]
  (map->TransformValue {:translate translate
                        :rotate rotate
                        :scale scale}))

(defprotocol AttrValue
  (attr->str [this]))

(extend-protocol AttrValue
  PersistentArrayMap
  (attr->str [this] (str this))
  ColorStringValue
  (attr->str [this] (.-value this))
  ColorRGBValue
  (attr->str [this] (str "rgb(" (.-r this) ", " (.-g this) ", " (.-b this) ")"))
  StringValue
  (attr->str [this] (.-value this))
  TranslateValue
  (attr->str [this]
    (if (and (:x this) (:y this))
      (str "translateX(" (:x this 0) ") translateY(" (:y this 0) ") ")
      ""))
  RotateValue
  (attr->str [this]
    (if (and (:x this) (:y this) (:y this))
      (str "rotate("  (:z this) "deg) ")
      ""))
  ScaleValue
  (attr->str [this]
    (if (and (:x this) (:y this))
      (str "scale(" (:x this) "," (:y this) ") ")
      ""))
  TransformValue
  (attr->str [this]
    (let [tr (map->TranslateValue (:translate this))
          rot  (map->RotateValue (:rotate this))
          sc (map->ScaleValue (:scale this))]
      (str (if tr (attr->str tr) "")
           (if rot (attr->str rot) "")
           (if sc (attr->str sc) "")))))

(defn rgb-list [^ColorRGBValue rgb-color]
  (list (.-r rgb-color) (.-g rgb-color) (.-b rgb-color)))

(extend-protocol interpolate/IInterpolate
  ColorRGBValue
  (interpolate/-interpolate [start end]
    (fn [t]
      (let [[r g b] ((interpolate/interpolate (rgb-list start) (rgb-list end)) t)]
        (->ColorRGBValue r g b))))
  TranslateValue
  (interpolate/-interpolate [start end]
    (fn [t]
      (let [[x y] ((interpolate/interpolate (list (:x start) (:y start))
                                            (list (:x end) (:y end)))
                   t)]
        (->TranslateValue x y))))
  ScaleValue
  (interpolate/-interpolate [start end]
    (fn [t]
      (let [[x y] ((interpolate/interpolate (list (:x start) (:y start))
                                            (list (:x end) (:y end)))
                   t)]
        (->ScaleValue x y))))
  RotateValue
  (interpolate/-interpolate [start end]
    (fn [t]
      (let [[x y z] ((interpolate/interpolate (list (:x start) (:y start) (:z start))
                                              (list (:x end) (:y end) (:z end)))
                     t)]
        (->RotateValue x y z))))
  TransformValue
  (interpolate/-interpolate [start end]
    (fn [t]
      (let [sc-intpl (interpolate/interpolate (:scale start) (:scale end))
            tr-intpl (interpolate/interpolate (:translate start) (:translate end))
            rot-intpl (interpolate/interpolate (:rotate start) (:rotate end))]
        (map->TransformValue {:translate (tr-intpl t)
                              :rotate (rot-intpl t)
                              :scale (sc-intpl t)})))))

(extend-protocol interpolate/IFresh
  ColorRGBValue
  (interpolate/fresh [x]
    (->ColorRGBValue 0 0 0))
  ScaleValue
  (interpolate/fresh [x]
    (scale-value 1))
  TranslateValue
  (interpolate/fresh [x]
    (translate-value 0 0))
  RotateValue
  (interpolate/fresh [x]
    (rotate-value 0))
  TransformValue
  (interpolate/fresh [x]
    (transform-value {})))

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

(defn set-opacity [obj opacity]
  ;; (. obj (setAttribute "opacity" opacity))
  (set! (.. obj -style -opacity) opacity)
  obj)
(defn set-scale [obj scale]
  (. obj (setAttribute "transform-origin" "50% 50%"))
  (. obj (setAttribute "transform" (str "scale(" scale ", " scale ")")))
  obj)

(defn set-transform [obj ^TransformValue t]
  (let [s (.-style obj)]
    (set! (. s -transformOrigin) "50% 50%")
    (set! (. s -transform) (attr->str t))
    ;; (. obj (setAttribute "transform-origin" "50% 50%"))
    ;; (. obj (setAttribute "transform" (attr->str t)))
    obj))

(defn get-logo-chars [svg]
  (->> (for [c (mapv #(str "char" %) (range 1 8))]
         (. svg (querySelector (str "#" c))))
       (into [])))
(defn- get-logo-path [svg]
  (. svg (querySelector "#logo")))

(defn init-chars! [svg]
  (let [logo-text (.. svg (querySelector "#logo-text"))]
    (set! (.. logo-text -style -opacity) 1)))
(comment
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
    ([:svg from to element-id]
     (generic-tween svg from to element-id 0 true))))

(defn- ping-pong [values]
  (cycle (concat values (reverse values))))

(defn transition-fn [from to opts
                     update-fn delay yoyo?]
  "creates a transition animation
  from: initial state
  to: target state
  opts: transition options, default is {:duration 500}
  update-fn: function that receives interpolated value and does an update
  delay: amount of ms to wait before starting animation
  yoyo?: loop the animation back and forth"
  (let [duration (get opts :duration 500)
        times (concat
               (range 0 1 (/ 50 duration))
               [1])
        intpl (interpolate/interpolate from to)
        values (-> intpl
                   (ease/wrap (ease/ease :cubic-in-out))
                   (interpolate/into-lazy-seq times))]
        ;; cancelled? (reagent/atom false)]
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
    ;; cancelled?))

(defn stagger-out-opacity [svg delay duration delta]
  ;; (doall
   (doseq [[i c] (mapv vector (range) (get-logo-chars svg))]
     (transition-fn 1.0
                    0.0
                    {:duration duration}
                    (fn [o]
                      (-> c (set-opacity o)))
                    (+ delay (* i delta))
                    false)))

(defn- set-chars-invisible! [svg]
  (doseq [[i c] (map-indexed #(vector %1 %2) (get-logo-chars svg))]
    (set! (.. c -style -opacity) 0)))
(defn perform-animation
  "Main logo animation"
  [svg]
  (let [the-chars (get-logo-chars svg)
        from      {:opacity 0
                   :transform (transform-value
                               {:scale (scale-value 1.2)
                                :rotate (rotate-value 90)})}
                                ;; :translate (translate-value -10  -10)})}
                   ;; :scale   1.2}
        to        {:opacity 1
                   :transform (transform-value
                               {:scale (scale-value 1.0)
                                :rotate (rotate-value 0)})}
                                ;; :translate (translate-value 0 0)})}
                   ;; :scale   1.0}
        duration 1000
        logo-rot-chan (transition/transition -30
                                             30
                                             {:duration 500})]
    ;; (init-chars! svg)
    (set-chars-invisible! svg)
    (transition-fn 0.2 1.0 {:duration 3000
                            :easing :cubic}
                   (fn [s]
                     (set-scale (get-logo-path svg) s))
                   1000
                   false)
    (transition-fn {:stroke (->ColorRGBValue 0 0 0)
                    :stroke-width 0.1}
                   {:stroke (->ColorRGBValue 255 255 255)
                    :stroke-width 12.0}
                   {:duration 3000}
                   (fn [{:keys [stroke stroke-width]}]
                     (let [l (get-logo-path svg)
                           s (.-style l)]
                       (set! (.-stroke s) (attr->str stroke))
                       (set! (.-strokeWidth s) stroke-width)))
                   4000
                   false)
    (transition-fn {:stroke (->ColorRGBValue 0 0 0)
                    :stroke-width 0.0}
                   {:stroke (->ColorRGBValue 0 0 0)
                    :stroke-width 6.0}
                   {:duration 3000}
                   (fn [{:keys [stroke stroke-width]}]
                     (let [ls (get-logo-chars svg)]
                       (doseq [c the-chars
                               :let [s (.-style c)]]
                         (set! (.-stroke s) (attr->str stroke))
                         (set! (.-strokeWidth s) stroke-width))))
                   8000 false)

    (transition-fn 1 0 {:duration 3000}
                   (fn [o]
                     (set! (.. (get-logo-path svg) -style -opacity) o))
                   8000
                   false)

    ;; stagger the logo chars by half a sec
    (doseq [[i c] (map-indexed #(vector %1 %2) the-chars)]
      (transition-fn from to
                     {:duration duration}
                     (fn [{:keys [opacity ^TransformValue transform]}]
                       (-> c
                           (set-opacity opacity)
                           (set-transform (transform-value transform))))
                           ;; (set-scale scale)))
                     (* i 500)
                     false))
    (stagger-out-opacity svg 11000 1000 200)))

(defn logo-animation []
  (let [l (reagent/atom nil)
        element-id (str (random-uuid))]
    (reagent/create-class
     {:component-will-unmount
      (fn [this]
        (gevents/unlistenByKey (.. ^js this ^js -state ^js -listenerKey))
        (. this (setState nil))
        (println "unlistened event"))
      :initial-state {"listenerKey" nil}
      :component-did-mount
      (fn [this]
        (let [s (dom/getElement  element-id)
              l2 (gevents/listen ^js s ^js (. gevents/EventType -LOAD)
                                 (fn []
                                   (let [svg (.-contentDocument s)
                                         txt (. svg (querySelector "#logo-text"))
                                         logo (. svg (querySelector "#logo"))]
                                     (set! (.. txt -style -transformOrigin) "50% 50%")
                                     (set! (.. logo -style -transformOrigin) "50% 50%")
                                     (set! (.. (get-logo-path svg) -style -transformOrigin) "50% 50%")
                                     ;; (. (get-logo-path svg) (setAttribute "transform-origin" "50% 50%"))
                                     (perform-animation svg)
                                     (js/console.log "Loaded doc " txt))))]
          (. this (setState (clj->js {:listener-key l2})))
          (println "Got element " s)))

      :render (fn [this]
                [:div.my-inlined-svg
                 [:object
                  (stylefy/use-style
                   loader-logo
                   {:id element-id
                    :data "images/logo-2.svg"
                    :type "image/svg+xml"
                    :alt "funky animation"})]])})))



(defprotocol Animation
  (play! [this])
  (duration [this])
  (cancel! [this])
  (pause [this])
  (restart [this]))


(defrecord Tween [id from to delay ms update-fn cancel-chan])

(defn tween [from to update-fn delay ms]
  (let [id (random-uuid)
        c-ch (async/chan nil)]
    (->Tween id from to delay ms update-fn c-ch)))

(defrecord Timeline [id tweens delay ms])

(defprotocol ITimeline
  (then [this other-anim])
  (current-pos [this])
  (remove-from-timeline [this id-anim]))

(extend-protocol Animation
  Tween
  (play! [this]
    (transition-fn (:from this) (:to this) {:duration (:ms this)}
                   (:update-fn this) (:delay this) false)))
