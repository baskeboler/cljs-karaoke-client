(ns cljs-karaoke.components.progress-bar
  (:require [reagent.core]
            [stylefy.core :as stylefy]))

(def progress-bar-style
  {:width            "calc(100% - 1rem)"
   :display          :block
   :height           "0.8rem"
   :padding          0
   :margin           "0 0.5rem"
   :background-color "rgba(255,255,255, 0.7)"
   :border           "1px black solid"
   :border-radius    "0.3em"
   :box-shadow "0 0 1rem 0.1rem black"})

(def progress-bar-label-style
  {:position :absolute
   :bottom 0
   :left 0
   :margin 0
   :padding 0
   :width "100%"
   :text-align :center
   :z-index 2
   :color :white
   :text-shadow "0 0 3px black"})

(defn ^:export progress-bar-component
  "Generic progress bar component to replace html progress element which
  improves the value change animation by applying css transitions for smoother
  value changes"
  [& {:keys [max-value
             current-value
             color
             value-bar-style
             label
             style]
      :or   {max-value       100
             current-value   0
             color           :hotpink
             value-bar-style {}
             style {}
             label nil}}]
  (let [progress-value (if (and (<= current-value max-value)
                                (>= current-value 0))
                         (/ (* current-value 100.0) max-value)
                         0)
                                
        bar-style      (merge
                        {:width            (str progress-value "%")
                         :height           "100%"
                         :border-radius    "0.3em"
                         :margin           :none
                         :position         :relative
                         :display          :block
                         :background-color color
                         :transition       "0.3s width ease-out"}
                        value-bar-style)]
    [:div.progress-bar (stylefy/use-style (merge progress-bar-style style))
     [:span.progress-bar-value (stylefy/use-style bar-style)]
     (when label
       [:span (stylefy/use-style progress-bar-label-style) label])]))
