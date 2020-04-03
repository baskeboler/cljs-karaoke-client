(ns cljs-karaoke.editor.view
  (:require [re-frame.core :as rf]
            [goog.string :as gstr]
            [reagent.core :as reagent :refer [atom]]
            [stylefy.core :as stylefy]
            ;; [cljs-karaoke.components.autocomplete :refer [autocomplete-input]]
            [cljs-karaoke.editor.events :as editor-events]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.modals :as modals]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.views.playback :as playback]
            [cljs-karaoke.editor.subs :as editor-subs]
            [thi.ng.color.core :as color]
            [cljs-karaoke.styles :refer [default-page-styles]]
            [cljs-karaoke.lyrics :as lyrics]
            [clojure.string :as str]))
(declare print-frames-str)

(defn song-name-editor []
  (let [song-name (rf/subscribe [::editor-subs/song-name])
        editing?  (reagent/atom false)]
    (fn []
      [:span.song-name-editor
       (if @editing?
        [:div.field>div.control>input.input.is-large.is-primary
         {:value       @song-name
          :on-blur     #(swap! editing? not)
          :on-change   #(rf/dispatch [::editor-events/set-song-name (-> % .-target .-value)])
          :placeholder "Name of the song"}]
        [:p.subtitle.is-3
         {:on-click #(swap! editing? not)}
         (if-not (str/blank? @song-name)
           @song-name
           "unknown")
         [:i.fas.fa-fw.fa-edit.fa-sm]])])))
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

(defn export-btn []
  [:button.button
   {:on-click #(modals/show-export-text-info-modal
                {:title "Export synced lyrics"
                 :text (print-frames-str @(rf/subscribe [::editor-subs/frames]))})}
   "Export Lyrics"])

(defn import-frames-btn []
  [:button.button
   {:on-click (fn []
                (modals/show-input-text-modal
                 {:title "import lyrics"
                  :text ""
                  :on-submit #(rf/dispatch [::editor-events/load-frames %])}))}
   "import lyrics"])

(comment
  (def editor-styles
    {:background-color "rgba(255,255,255,0.7)"
     :padding          "2em 1em"
     :border-radius    "0.5em"}))

(defn segment-table [{:keys [result done? text remaining-text]}]
  [:table.table.is-fullwidth
   [:thead
    [:tr [:th "segment"] [:th "offset"]]]
   [:tbody
    (doall
     (for [[i s] (map-indexed vector result)]
       ^{:key (str "row_" i)}
       [:tr
        [:td s]
        [:td 0]]))]
   [:tfoot
    [:tr [:th "is done?"] [:td (if done? "YES" "NO")]]
    [:tr [:th "remaining"] [:td (gstr/format "%s (%d characters)"  remaining-text (count remaining-text))]]]])

(def my-colors
  (repeatedly #(-> (color/random-rgb)
                   color/as-css)))

(defn frames-table []
  [:table.table.is-narrow.is-fullwidth
   [:thead
    [:tr
     [:th "id"]
     [:th "text"]
     [:th "offset"]
     [:th "actions"]]]
   [:tbody
    (doall
     (for [{:keys [id events offset]} @(rf/subscribe [::editor-subs/frames])
           :let [active-frame (rf/subscribe [::editor-subs/active-frame])]]
       ^{:key (str "frame_row_" id)}
       [:tr {:class (when (= (:id @active-frame) id) "is-selected")}
        [:td (apply str  (take 8 id))]
        [:td (apply str (map :text events))]
        [:td>div (gstr/format "%1.3f s" (/ offset 1000))]
        [:td
         [:button.button.is-small
          {:on-click #(set! (.-currentTime @(rf/subscribe [::s/audio]))
                            (/ offset 1000))}
          "jump to position"]
         [:button.button.is-small
          {:on-click #(rf/dispatch [::editor-events/delete-frame id])}
          "delete"]
         [:button.button.is-small
          {:on-click #(rf/dispatch [::editor-events/edit-frame id])}
          "edit"]]]))]])
(defn- load-local-audio [evt]
  (let [f (.. ^js evt -currentTarget -files (item 0))
        reader (js/FileReader.)]
    (set! (. reader -onload)
          (fn [e]
            (set! (.-src @(rf/subscribe [::s/audio]))
                  (.. e -target -result))))
    (.. reader (readAsDataURL f))))
(defn frame-text-editor []
  [:div.columns>div.column.is-full
   [:div.columns>div.column.is-full>div.file.has-name>label.file-label
    [:input.file-input
     {:type :file
      :on-change #(load-local-audio %)}]
    [:span.file-cta
     [:span.file-icon>i.fas.fa-upload]
     [:span.file-label "Choose an audio file to sync the lyrics to"]]]
     ;; [:span.file-name "file name"]]
   [:div.columns>div.column.is-full>textarea.textarea.is-primary.is-full
    {:value     @(rf/subscribe [::editor-subs/current-frame-property :text])
     :lines     10
     :on-change #(rf/dispatch [::editor-events/set-current-frame-property :text  (-> % .-target .-value)])}]
   [:button.button.is-primary.is-fullwidth
    {:on-click #(do
                  (rf/dispatch [::editor-events/set-current-frame-property :text-done? true])
                  (rf/dispatch [::editor-events/editor-action :confirm-text])
                  (rf/dispatch [::editor-events/set-current-frame-property :segments
                                (editor-events/get-segments
                                 @(rf/subscribe [::editor-subs/current-frame-property :segment-sizes])
                                 @(rf/subscribe [::editor-subs/current-frame-property :text]))]))}

    "confirm frame text"]])

(defn segments-display [{:keys [result done? text remaining-text]}]
  [:div.is-size-2
   [:i.fas.fa-fw.fa-arrow-right]
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
      ^{:key (str "segment_selector_" i)}
      [:span.segment-character
       (stylefy/use-style
        {:display :inline-block
         ::stylefy/mode {:hover {:border-right "1px solid red"}}}
        {:on-click #(on-new-segment-select-fn (inc i))})
       (-> c
           (clojure.string/replace #" " "&#160;")
           (gstr/unescapeEntities))]))])

(defn segment-timing-editor []
  (let [segment-offsets (rf/subscribe [::editor-subs/current-frame-property :segment-offsets])
        segments        (rf/subscribe [::editor-subs/current-frame-property :segments])
        current-time    (rf/subscribe [::s/song-position-ms])
        active-segment  (rf/subscribe [::editor-subs/active-segment])]
    [:div.columns>div.column
     [:div.is-size-2.mb-2
      (doall
       (for [[i seg] (map-indexed vector (vec
                                          (sort-by :offset
                                                   (vec
                                                    (vals @segments)))))
             :let    [{:keys [text id]} seg]]
         ^{:key (str "segment_timing_" i)}
         [:span.segment-timing-part
          (stylefy/use-style
           (merge
            {:display :inline-block}
             ;; :margin  "1em 0"}
            (cond
              (= i (count @segment-offsets))     {:border-bottom "solid 4px red"}
              (and ((comp not nil?) @active-segment)
                   (= id (:id @active-segment))) {:border-top "solid 4px blue"}
              :otherwise                         {})))
          (-> text
              (clojure.string/replace #" " "&#160;")
              (gstr/unescapeEntities))]))]
     [:div.columns>div.column>div.buttons.field.has-addons
      [:div.control>button.button.is-large.is-danger
       {:on-click #(rf/dispatch [::editor-events/set-current-frame-property
                                 :segment-offsets
                                 (conj (vec @segment-offsets) @current-time)])}
       "sync now!"]
      [:div.control>button.button.is-large.is-warning
       {:on-click #(rf/dispatch [::editor-events/set-current-frame-property
                                 :segment-offsets
                                 (butlast @segment-offsets)])}
       "undo"]]]))

(defn segment-timing-preview [])

(defn- segment-selection-component []
  (let [text          (rf/subscribe [::editor-subs/current-frame-property :text])
        segment-sizes (rf/subscribe [::editor-subs/current-frame-property :segment-sizes])]
    [:div.columns
     [:div.column.is-full
      [segments-display (editor-events/get-segments @segment-sizes @text)]
      [segment-selector
       {:text                     (-> (editor-events/get-segments @segment-sizes @text)
                                      :remaining-text)
        :on-new-segment-select-fn #(rf/dispatch [::editor-events/set-current-frame-property
                                                 :segment-sizes
                                                 (conj @segment-sizes %)])}]
      [:div.field.is-grouped.has-addons
       [:div.control.has-icon
        [:button.button.is-warning
         {:on-click #(rf/dispatch [::editor-events/set-current-frame-property :segment-sizes
                                   ((comp vec butlast) @segment-sizes)])}

         "remove segment"]]]
      [:button.button.is-primary.is-fullwidth
       {:on-click #(do
                     (rf/dispatch [::editor-events/set-current-frame-property :segments-done? true])
                     ;; (reset! segments-done? true)
                     (rf/dispatch [::editor-events/set-current-frame-property :segments
                                   (->>
                                    (editor-events/get-segments @segment-sizes @text)
                                    :result
                                    (map-indexed (fn [i s] [i {:id i :text s}]))
                                    (into {}))])
                     (rf/dispatch [::editor-events/editor-action :confirm-segments]))
        ;; (reset! segments (editor-events/get-segments @segment-sizes @text)))}
        :disabled (not
                   @(rf/subscribe [::editor-subs/segments-ready?]))}
                    ;; (:done? (editor-events/get-segments @segment-sizes @text)))}

       "confirm segments"]]]))
(defn- segment-timing-component []
  [:div.columns>div.column.is-12
   [:div.has-background-primary.has-text-light.has-text-centered.is-size-2
    [:i.fas.fa-fw.fa-clock.has-text-warning]
    (gstr/format
     "%1.3f"
     @(rf/subscribe [::s/player-current-time]))]

   [segment-timing-editor]
   [:button.button.is-fullwidth.is-warning
    {:on-click #(do
                  (rf/dispatch [::editor-events/add-frame])
                  (rf/dispatch [::editor-events/editor-action :confirm-timing]))
     :disabled (not @(rf/subscribe [::editor-subs/segment-timings-ready?]))}
    "add frame"]])
(defn editor-mode-title []
  (let [mode (rf/subscribe [::editor-subs/mode-title])]
    [:div.title.is-text-2 @mode]))

(defn- image []
  [:figure.image {:style
                  {:width "256px"}}
   [:img {:src   "/images/art_window.svg"}]])

(defn  ^:export editor-component []
  (let [text            (rf/subscribe [::editor-subs/current-frame-property :text])
        text-done?      (rf/subscribe [::editor-subs/current-frame-property :text-done?])
        segments-done?  (rf/subscribe [::editor-subs/current-frame-property :segments-done?])
        offsets-done?   (rf/subscribe [::editor-subs/current-frame-property :offsets-done?])
        segment-sizes   (rf/subscribe [::editor-subs/current-frame-property :segment-sizes])
        segment-offsets (rf/subscribe [::editor-subs/current-frame-property :segment-offsets])
        segments        (rf/subscribe [::editor-subs/current-frame-property :segments])
        size            (rf/subscribe [::editor-subs/current-frame-property :segment-size])]
      [:div.editor
       (stylefy/use-style default-page-styles)
       [:div.columns.is-vcentered
        [:div.column.is-hidden-mobile
         [image]]
        [:div.column>h1.title.is-1 "Lyrics Editor"]
        [:div.column
         [song-name-editor]]]
       [:div.columns
        [:div.column.is-one-third
         [:div.columns>div.column
          [export-btn]
          [import-frames-btn]]
         [:div.columns>div.column
          [frames-table]]]
        [:div.column.is-two-thirds
         [:div.columns>div.column.is-12
          [song-progress]
          [playback/playback-controls]]
         [editor-mode-title]
         [:div.box
          (condp = @(rf/subscribe [::editor-subs/current-state])
            :text-entry        [frame-text-editor]
            :segment-selection [segment-selection-component]
            :segment-timing    [segment-timing-component])]

           ;; (when (= :segment-timing @(rf/subscribe [::editor-subs/current-state]))
  
         [:button.button.is-fullwidth
          {:on-click #(do
                        (rf/dispatch [::editor-events/reset-frame])
                        (rf/dispatch [::editor-events/editor-action :reset]))}
          "reset"]]]]))

(defn print-frames-str [frames]
  (pr-str (map lyrics/->map frames)))
