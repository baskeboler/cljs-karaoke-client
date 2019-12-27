(ns cljs-karaoke.views.editor
  (:require [re-frame.core :as rf]
            [goog.string :as gstr]
            [reagent.core :as reagent :refer [atom]]
            [stylefy.core :as stylefy]
            [cljs-karaoke.components.autocomplete :refer [autocomplete-input]]
            [cljs-karaoke.events.editor :as editor-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.views.playback :as playback]))

()
(defn song-progress []
  (let [dur (rf/subscribe [::s/song-duration])
        cur (rf/subscribe [::s/song-position])]
    (fn []
      [:progress.progress.is-small.is-primary
       {:max (if (number? @dur) @dur 0)
        :value (if (number? @cur) @cur 0)}
       (str (if (pos? @dur)
              (long (* 100 (/ @cur @dur)))
              0) "%")])))


(def editor-styles
  {:background-color "rgba(255,255,255,0.7)"
   :padding          "2em 1em"
   :border-radius    "0.5em"})

(defn segment-table [{:keys [result done? text remaining-text]}]
  [:table.table.is-fullwidth
   [:thead
    [:tr [:th "segment"] [:th "offset"]]]
   [:tbody
    (doall
     (for [[i s] (map-indexed vector result)]
       ^{:key (str "row_" i)} [:tr [:td s] [:td 0]]))]
   [:tfoot
    [:tr [:th "is done?"] [:td (if done? "YES" "NO")]]
    [:tr [:th "remaining"] [:td (gstr/format "%s (%d characters)"  remaining-text (count remaining-text))]]]])

(defn  ^:export editor-component []
  (let [text            (atom "hola como te va?")
        text-done?      (atom false)
        segments-done?  (atom false)
        offsets-done?   (atom false)
        segment-sizes   (atom [])
        segment-offsets (atom [])
        segments        (atom nil)
        size            (atom 0)]
    (fn []
      [:div.editor.container-fluid
       (stylefy/use-style editor-styles)
       [:div.title "Lyrics Editor"]
       [:div.columns>div.column.is-12
        [song-progress]
        [playback/playback-controls]
        [autocomplete-input identity (take 10 @(rf/subscribe [::s/available-songs]))]]
       (when-not @text-done?
         [:div.columns
          [:div.column.is-full
           [:textarea.textarea.is-primary.is-full
            {:value     @text
             :lines     10
             :on-change #(reset! text (-> % .-target .-value))}]
           [:button.button.is-primary.is-fullwidth
            {:on-click #(do
                          (reset! text-done? true)
                          (reset! segments (editor-events/get-segments @segment-sizes @text)))}

            "confirm frame text"]]])
       (when (and (not @segments-done?)
                  @text-done?)
         [:div.columns
          [:div.column.is-full
           [:div.has-text-dark.is-size-3 @text]
           [:div.field.is-grouped.has-addons
            [:div.control
             [:input.input
              {:type      :number
               :value     @size
               :on-change #(reset! size (-> % .-target .-value int))}]]
            [:div.control
             [:button.button.is-primary
              {:on-click #(do
                            (swap! segment-sizes conj @size)
                            (reset! size 0))}
              [:i.fas.fa-fw.fa-plus]]]
            [:div.control.has-icon
             [:button.button.is-warning
              {:on-click #(swap! segment-sizes (comp vec butlast))}
               
              "remove segment"]]]
           [:div (pr-str @segment-sizes)]
           [segment-table (editor-events/get-segments @segment-sizes @text)]
           [:button.button.is-primary.is-fullwidth
            {:on-click #(do
                          (reset! segments-done? true)
                          (reset! segments (->>
                                             (editor-events/get-segments @segment-sizes @text)
                                             :result
                                             (map-indexed (fn [i s] [i {:id i :text s}]))
                                             (into {}))))
                          ;; (reset! segments (editor-events/get-segments @segment-sizes @text)))}
             :disabled (not (:done? (editor-events/get-segments @segment-sizes @text)))}

            "confirm segments"]]])
       (when @segments-done?
         [:div.columns>div.column.is-12
          [:div.has-text-primary.is-size-2
           @(rf/subscribe [::s/player-current-time])]
          [:table.table
           (doall
            (for [[k {:keys [id text offset]}] @segments]
              ^{:keys (str "row_offset_" id)}
              [:tr
               [:td id]
               [:td text]
               [:td offset]
               [:td [:button.button.is-small
                     {:on-click #(swap! segments
                                        assoc-in
                                        [id :offset]
                                        @(rf/subscribe [::s/player-current-time]))}
                     "now!"]]]))]])
       [:button.button.is-fullwidth
        {:on-click #(do
                      (reset! text-done? false)
                      (reset! segments-done? false)
                      (reset! offsets-done? false))}
        "reset"]])))
