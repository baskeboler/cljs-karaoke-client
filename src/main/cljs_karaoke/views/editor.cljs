(ns cljs-karaoke.views.editor
  (:require [re-frame.core :as rf]
            [goog.string :as gstr]
            [reagent.core :as reagent :refer [atom]]
            [stylefy.core :as stylefy]
            [cljs-karaoke.components.autocomplete :refer [autocomplete-input]]
            [cljs-karaoke.events.editor :as editor-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.views.playback :as playback]
            [cljs-karaoke.subs.editor :as editor-subs]
            [thi.ng.color.core :as color]
            [cljs-karaoke.lyrics :as lyrics]))
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
(def my-colors
  (repeatedly #(-> (color/random-rgb)
                   color/as-css)))

(defn segments-display [{:keys [result done? text remaining-text]}]
  [:div.is-size-2
   (doall
    (for [[i color seg] (map vector
                             (range)
                             my-colors
                             result)]
      ^{:key (str "segment_number_" i)}
      [:span
       (stylefy/use-style {:color @color})
       seg]))])
(defn segment-selector [{:keys [text on-new-segment-select-fn]}]
  [:div.has-text-primary.is-size-2
   (doall
    (for [[i c] (map-indexed vector text)]
      [:span.segment-character
       (stylefy/use-style
        {::stylefy/mode {:hover {:border-right "1px solid red"}}}
        {:on-click #(on-new-segment-select-fn (inc i))})
       c]))])

(defn  ^:export editor-component []
  (let [text            (rf/subscribe [::editor-subs/current-frame-property :text])
        text-done?      (rf/subscribe [::editor-subs/current-frame-property :text-done?])
        segments-done?  (rf/subscribe [::editor-subs/current-frame-property :segments-done?])
        offsets-done?   (rf/subscribe [::editor-subs/current-frame-property :offsets-done?])
        segment-sizes   (rf/subscribe [::editor-subs/current-frame-property :segment-sizes])
        segment-offsets (rf/subscribe [::editor-subs/current-frame-property :segment-offsets])
        segments        (rf/subscribe [::editor-subs/current-frame-property :segments])
        size            (rf/subscribe [::editor-subs/current-frame-property :segment-size])]
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
             :on-change #(rf/dispatch [::editor-events/set-current-frame-property :text  (-> % .-target .-value)])}]
           [:button.button.is-primary.is-fullwidth
            {:on-click #(do
                          (rf/dispatch [::editor-events/set-current-frame-property :text-done? true])
                          (rf/dispatch [::editor-events/set-current-frame-property :segments
                                        (editor-events/get-segments @segment-sizes @text)]))}

            "confirm frame text"]]])
       (when (and (not @segments-done?)
                  @text-done?)
         [:div.columns
          [:div.column.is-full
           ;; [:div.has-text-dark.is-size-3 @text]
           [segments-display (editor-events/get-segments @segment-sizes @text)]
           [segment-selector
            {:text (-> (editor-events/get-segments @segment-sizes @text)
                       :remaining-text)
             :on-new-segment-select-fn #(rf/dispatch [::editor-events/set-current-frame-property
                                                      :segment-sizes
                                                      (conj @segment-sizes %)])}]
           [:div.field.is-grouped.has-addons
            [:div.control
             [:input.input
              {:type      :number
               :value     @size
               :on-change #(rf/dispatch [::editor-events/set-current-frame-property :segment-size
                                         (-> % .-target .-value int)])}]]
            [:div.control
             [:button.button.is-primary
              {:on-click #(do
                            (rf/dispatch [::editor-events/set-current-frame-property :text-done? true])
                            (rf/dispatch [::editor-events/set-current-frame-property :segment-sizes (conj @segment-sizes @size)])
                            (rf/dispatch [::editor-events/set-current-frame-property :segment-size 0]))}

                            ;; (swap! segment-sizes conj @size)
                            ;; (reset! size 0))}
              [:i.fas.fa-fw.fa-plus]]]
            [:div.control.has-icon
             [:button.button.is-warning
              {:on-click #(rf/dispatch [::editor-events/set-current-frame-property :segment-sizes
                                        ((comp vec butlast) @segment-sizes)])}

              "remove segment"]]]
           [:div (pr-str @segment-sizes)]
           [segment-table (editor-events/get-segments @segment-sizes @text)]
           [:button.button.is-primary.is-fullwidth
            {:on-click #(do
                          (rf/dispatch [::editor-events/set-current-frame-property :segments-done? true])
                          ;; (reset! segments-done? true)
                          (rf/dispatch [::editor-events/set-current-frame-property :segments
                                        (->>
                                         (editor-events/get-segments @segment-sizes @text)
                                         :result
                                         (map-indexed (fn [i s] [i {:id i :text s}]))
                                         (into {}))]))
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
              ^{:keys (str "row_offset_" k)}
              [:tr
               [:td id]
               [:td text]
               [:td offset]
               [:td [:button.button.is-small
                     {:on-click #(rf/dispatch [::editor-events/set-current-frame-property :segments
                                               (update @segments
                                                       k
                                                       assoc-in
                                                       [:offset]
                                                       @(rf/subscribe [::s/player-current-time]))])}
                     "now!"]]]))]
          [:button.button.is-fullwidth.is-warning
           {:on-click #(rf/dispatch [::editor-events/add-frame])}
           "add frame"]])
       [:button.button.is-fullwidth
        {:on-click #(do
                      (rf/dispatch [::editor-events/reset-frame]))}

                      ;; (reset! text-done? false)
                      ;; (reset! segments-done? false)
                      ;; (reset! offsets-done? false))}
        "reset"]])))
