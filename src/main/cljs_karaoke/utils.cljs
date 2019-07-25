(ns cljs-karaoke.utils
  (:require [reagent.core :as reagent :refer [atom] :include-macros true]
            [re-frame.core :as rf :include-macros true]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.http-relay :as relay-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.subs.http-relay :as hr]))
(defn modal-card-dialog [{:keys [title content footer]}]
  [:div.modal.is-active
   [:div.modal-background]
   [:div.modal-card
    [:header.modal-card-head
     [:p.modal-card-title title]
     [:button.delete
      {:aria-label "close"
       :on-click #(rf/dispatch [::events/modal-pop])}]]
    [:section.modal-card-body
     content]
    (when-not (nil? footer)
      [:footer.modal-card-foot
        footer])]])

(defn modals-component []
  [:div.modals
   (for [m @(rf/subscribe [::s/modals])]
     m)])

(defn select-element-text [element-id]
  (let [element (. js/document (getElementById element-id))
        text-range (. js/document (createRange))
        selection (. js/window (getSelection))]
    (-> text-range (.selectNodeContents element))
    (-> selection (.removeAllRanges))
    (-> selection (.addRange text-range))))


(defn show-export-sync-info-modal []
  (let [data @(rf/subscribe [::s/custom-song-delay-for-export])
        modal (modal-card-dialog
               {:title "Export sync data"
                :content [:div.export-sync-data-content
                          [:div.field>div.control
                           [:textarea.textarea.is-primary
                            {:id "sync-info-textarea"
                             :value data}]]]
                           
                :footer nil})]
    (rf/dispatch [::events/modal-push modal])))

(defn show-remote-control-id []
  (let [listener-id @(rf/subscribe [::hr/http-relay-listener-id])
        modal (modal-card-dialog
               {:title "Remote control info"
                :content [:div.remote-control-info-content
                          [:div.field>div.control
                           [:textarea.textarea.is-primary
                            {:id "remote-control-id"
                             :value listener-id}]]]
                :footer nil})]
    (rf/dispatch [::events/modal-push modal])))

(defn remote-control-settings []
  (let [value (reagent/atom  "")]
    [:div.remote-control-settings-content
     [:div.field>div.control
      [:input.input.is-primary
       {:id "remote-control-id"
        :type :text
        :placeholder "Enter the connection code for the remote karaoke"
        :value @(rf/subscribe [::hr/remote-control-id])
        :on-change #(do
                      (rf/dispatch  [::relay-events/set-remote-control-id (-> % .-target .-value)]))}]]]))

(defn show-remote-control-settings []
  (let [modal (modal-card-dialog
               {:title "Set the remote screen id"
                :content [remote-control-settings]
                :footer nil})]
    (rf/dispatch [::events/modal-push modal])))

(defn ratings-input []
  [:div.control
   (let [ratings-vals (map inc (range 5))]
     (for [r ratings-vals]
       [:label.radio
        [:input {:type :radio
                 :name :answer}
         r]]))])
