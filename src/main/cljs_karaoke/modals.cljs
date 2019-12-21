(ns cljs-karaoke.modals
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.subs :as s]))
(defn modal-card-dialog [{:keys [title content footer]}]
  [:div.modal.is-active
   {:key (random-uuid)}
   [:div.modal-background]
   [:div.modal-card
    [:header.modal-card-head
     [:p.modal-card-title title]
     [:button.delete
      {:aria-label "close"
       :on-click #(rf/dispatch [::modal-events/modal-pop])}]]
    [:section.modal-card-body
     content]
    (when-not (nil? footer)
      [:footer.modal-card-foot
       footer])]])

(defn modals-component []
  [:div.modals
   (for [m @(rf/subscribe [::s/modals])]
     m)])


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
    (rf/dispatch [::modal-events/modal-push modal])))
