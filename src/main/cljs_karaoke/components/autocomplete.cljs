(ns cljs-karaoke.components.autocomplete
  (:require [reagent.core :as reagent :refer [atom]]))

(defn ^:export autocomplete-input
  ([on-change-fn suggestions on-select-fn]
   (let [value   (atom "")
         focused (atom false)]
     (fn []
       [:div.dropdown
        {:class (when (and
                       @focused
                       ((comp not empty?) suggestions)
                       "is-active"))}
        [:div.dropdown-trigger
         [:input.input
          {:on-blur   #(reset! focused false)
           :onFocus  #(reset! focused true)
           :value     @value
           :on-change (fn [evt]
                        (reset! value (-> evt .-target .-value))
                        (on-change-fn (-> evt .-target .-value)))}]]
        [:div.dropdown-menu {:role :menu}
         [:div.dropdown-content
          (doall
           (for [[i s] (map-indexed vector suggestions)]
             ^{:key (str "suggestion_" (hash s))}
             [:a.dropdown-item
              {:on-click (fn []
                           (reset! value s)
                           (on-select-fn s))}
              s]))]]])))
  ([on-change-fn suggestions]
   [autocomplete-input on-change-fn suggestions identity])
  ([on-change-fn]
   [autocomplete-input on-change-fn []])
  ([]
   [autocomplete-input identity]))
