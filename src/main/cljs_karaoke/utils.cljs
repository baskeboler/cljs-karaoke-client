(ns cljs-karaoke.utils
  (:require [reagent.core :as reagent :refer [atom] :include-macros true]
            [re-frame.core :as rf :include-macros true]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.subs.http-relay :as hr]))

(defn select-element-text [element-id]
  (let [element (. js/document (getElementById element-id))
        text-range (. js/document (createRange))
        selection (. js/window (getSelection))]
    (-> text-range (.selectNodeContents element))
    (-> selection (.removeAllRanges))
    (-> selection (.addRange text-range))))

(defn ratings-input []
  [:div.control
   (let [ratings-vals (map inc (range 5))]
     (for [r ratings-vals]
       [:label.radio
        [:input {:type :radio
                 :name :answer}
         r]]))])

(defn copy-text-to-clipboard [text]
  (let [elem-id (str (random-uuid))
        elem    (. js/document (createElement "input"))
        body    (.-body js/document)]
    (set! (.-value elem) text)
    (set! (.-id elem) elem-id)
    (. body (appendChild elem))
    (select-element-text elem-id)
    ;; (. elem (setSelectionRange 0 99999))
    (. js/document (execCommand "copy"))
    (. body (removeChild elem))))

       
(defn icon-button
  ([icon button-type callback enabled?]
   [:div.control
    [:p.control
     [:a.button.is-small
      {:class      [ (str "is-" button-type)]
       :disabled   (not @enabled?)
       :on-click   callback
       :aria-label icon
       :title      icon}
      [:span.icon.is-small
       [:i
        {:class ["fa" (str "fa-" icon)]}]]]]])
  ([icon button-type callback]
   (icon-button icon button-type callback (atom true))))
