(ns cljs-karaoke.views.billboards
  (:require [re-frame.core :as rf]
            [stylefy.core :as stylefy]
            [cljs-karaoke.subs.billboards :as b]
            [cljs-karaoke.events.billboards :as billboard-events]))
(def billboard-exit-styles
  {:animation-name            "slide-out-blurred-left"
   :animation-duration        "1s"
   :animation-timing-function :ease
   :animation-fill-mode       :both
   :animation-iteration-count 1})

(def billboard-enter-styles
  {:animation-name            "puff-in-top"
   :animation-duration        "1s"
   :animation-timing-function :ease
   :animation-fill-mode       :both
   :animation-iteration-count 1})

(def billboard-container-styles
  {:display        :flex
   :flex-direction :column
   :position       :fixed
   :top            "15vh"
   :left           "5vw"
   :width          "90vw"
   :min-height     "60vh"
   :background-color "rgba(0,0,0,.5)"})

(def billboard-styles
  {:position         :relative
   :display          :block
   :background-color :transparent})

(declare screen-width get-font-size)

(def display-font-family "Plyfair Display, serif")

(defn song-name-display-styles
  [visible? text]
  (let [w (screen-width)
        s (get-font-size :text text
                         :font-family display-font-family
                         :min-pixel-size 10
                         :font-weight "bold"
                         :display-pixel-width              (dec w))]
    {:font-size      (str s "px")
     :color          "#FA7C91" ; :black
     :text-align     :center
     :z-index        10001
     :animation-name (if visible?
                       "text-pop-up-bottom, puff-in-top"
                       "text-pop-up-bottom, slide-out-blurred-left")
     :font-family    display-font-family ;"Plyfair Display, serif"
     :font-weight    :bold}))

(defmulti render-billboard :type)

(defmethod render-billboard :song-name-display
  [{:keys [text visible? type]}]
  [:div.song-name-display.text-pop-up-bottom
   (stylefy/use-style
    (merge
     (if visible? billboard-enter-styles billboard-exit-styles)
     (song-name-display-styles visible? text)))
   text])

(defmethod render-billboard :h-scrolling-message
  [{:keys [text visible?]}]
  [:div.h-scrolling-message])

(defn billboard-component [{:keys [content visible?] :as b}]
  [:div.billboard
   (stylefy/use-style
    ;; billboard-styles)
     (merge
      billboard-styles
      (if  visible? billboard-enter-styles billboard-exit-styles)))
   [:div.billboard-content
    [render-billboard b]]])

(defn billboard-container [billboards]
  [:div.billboard-container.columns
   (for [b billboards]
     ^{:key (:id b)}
     [:div.column
      [billboard-component b]])])

(defn billboards-component []
  [billboard-container @(rf/subscribe [::b/billboards])])

(defn measure-text [text font]
  (let [canvas (. js/document (createElement "canvas"))
        ctx (. canvas (getContext "2d"))]
    (set! (. ctx -font) font)
    (.. ctx
        (measureText text)
        -width)))

(defn get-font-size [& {:keys [text font-family
                               font-style font-weight
                               min-pixel-size display-pixel-width]
                        :or   {min-pixel-size      10
                               font-style          "normal"
                               font-weight         "normal"
                               font-family         "verdana"
                               display-pixel-width 640}}]
  (let [font      (str font-weight " " min-pixel-size "px " font-family)
        min-width (measure-text text font)]
    (/ (* min-pixel-size display-pixel-width) min-width)))

(defn- screen-width [] (.. js/document -body -clientWidth))
