(ns cljs-karaoke.components.song-selector
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.ratom :refer [reaction]]
            [re-frame.core :as rf]
            [cljs-karaoke.subs :as subs]
            [clojure.string :as cstr]
            [cljs-karaoke.key-bindings :as k]
            [cljs-karaoke.modals :as modals]))

(defn filtered-songs
  [filter-text &{:keys [limit] :or {limit 20}}]
  (if (< (count filter-text) 3)
    []
    (take limit
          (filter #(cstr/includes? % (cstr/lower-case filter-text))
                  (map cstr/lower-case @(rf/subscribe [::subs/available-songs]))))))


(defn song-select
  [on-select-fn]
  (let [value       (reagent/atom "")
        suggestions (reaction  (filtered-songs @value :limit 5))
        selected?   (reagent/atom false)]
    (fn [on-select-fn]
      [:div.song-select.columns>div.column
       [:div.dropdown
         {:class (if-not (empty? @suggestions)
                  "is-active"
                  nil)}
        (when-not (or
                   @selected?
                   (empty? @suggestions))
          [:div.dropdown-menu {:role :menu}
           [:div.dropdown-content
            (for [s @suggestions]
              ^{:key (str "suggestion_" (hash s))}
              [:a.dropdown-item
               {:on-click #(do
                             (reset! selected? true)
                             (reset! value s)
                             (on-select-fn s))}
               s])]]
          [:div.select>select {:value value}
           :on-change (fn [evt]
                        (on-select-fn (-> evt .-target .-value)))
           (for [v @suggestions]
             [:option
              {:key (str "option_" (hash v))
               :value v}
              v])])
        [:div.dropdown-trigger>div.field.has-addons
          [:div.control.is-expanded>input.input
           (merge
            {:value     @value
             :on-focus  #(k/disable-keybindings!)
             :on-blur   #(k/enable-keybindings!)
             :class     (when @selected? "is-static")
             :on-change #(do
                           (reset! value (-> % .-target .-value))
                           (doto %
                             (.preventDefault)
                             (.stopPropagation)))}
            (when @selected? {:readOnly true}))]
          [:div.control>button.button.is-info
           {:on-click #(if @selected? (swap! selected? not))}
           (if @selected?
             "clear"
             "select")]]]])))
         
(declare song-select-panel)

(defn song-select-dialog [on-select]
  (let [selection (atom nil)]
    [modals/modal-card-dialog
     {:title "select song"
      :content [:div.song-select-content
                [:p.title "select a song"]
                [song-select-panel #(reset! selection %)]]
                ;; [song-select #(reset! selection %)]]
      :footer [modals/footer-buttons
               [:button.button.is-primary
                {:on-click (fn []
                             (on-select @selection)
                             (modals/pop-modal))}
                "select"]]}]))

  
(defn show-song-select-dialog []
  (modals/push-modal [song-select-dialog identity]))


(defn song-select-panel [on-select-fn]
  (let [value (atom "")
        suggestions (reaction (filtered-songs @value :limit 5))]
    (fn [on-select-fn]
      [:nav.panel
       [:p.panel-heading
        "Songs"]
       [:div.panel-block
        [:p.control.has-icons-left
         [:input.input
          {:type :text
           :placeholder "Search"
           :on-change #(reset! value (.. % -target -value))
           :value @value}]
         [:span.icon.is-left [:i.fas.fa-search {:aria-hidden true}]]]]
       [:p.panel-tabs
        [:a.is-active "All"]
        [:a "Public"]
        [:a "Private"]]
       (for [s @suggestions]
         [:a.panel-block
          {:on-click #(reset! value s)
           :class (if (= @value s) "is-active")}
          [:span.panel-icon
           [:i.fas.fa-music {:aria-hidden true}]]
          s])
       [:div.panel-block
        [:button.button.is-fullwidth.is-outlined
         {:on-click #(on-select-fn @value)}
         "select"]]])))
      
