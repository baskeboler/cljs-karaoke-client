(ns cljs-karaoke.modals
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events.modals :as modal-events]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.components.delay-select :refer [delay-select-component]]))
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
     (if (fn? content)
       [content]
       content)]
    (when-not (nil? footer)
      [:footer.modal-card-foot
       (if (fn? footer)
         [footer]
         footer)])]])

(defn modals-component []
  [:div.modals
   (for [m @(rf/subscribe [::s/modals])]
     m)])


(defn ^:export show-export-text-info-modal
  [{:keys [title text]}]
  (let [modal (modal-card-dialog
               {:title   title
                :content [:div.export-text-data-content
                          [:div.field>div.control
                           [:textarea.textarea.is-primary
                            {:id        (gensym ::export-text-info-modal)
                             :value     text
                             :read-only true}]]]
                :footer  nil})]
   (rf/dispatch [::modal-events/modal-push modal])))

(defn ^:export show-generic-tools-modal [{:keys [title content]}]
  (let [modal (modal-card-dialog
               {:title   title
                :content content
                :footer  [:div.footer-container
                          [:button.button.is-primary-outlined
                           {:on-click #(rf/dispatch [::modal-events/modal-pop])}
                           "Dismiss"]]})]
    (rf/dispatch [::modal-events/modal-push modal])))

(defn ^:export show-delay-select-modal []
  (show-generic-tools-modal {:title "Adjust lyrics delay"
                             :content [:div.delay-select-modal-content
                                       [:label "Select lyrics delay"]
                                       [delay-select-component]]}))

(defn ^:export show-input-text-modal
  [{:keys [title text on-submit]}]
  (let [input-text (reagent.core/atom "")
        modal      (modal-card-dialog
                    {:title   title
                     :content (fn []
                                [:div.input-text-data-content
                                 [:div.field>div.control
                                  [:textarea.textarea.is-primary
                                   {:id        (gensym ::input-text-modal)
                                    :value     @input-text
                                    :on-change #(reset! input-text (-> % .-target .-value))}]]])
                     :footer [:div.footer-container
                               [:button.button.is-primary
                                 {:on-click #(on-submit @input-text)}
                                 "OK"]]})]
      (rf/dispatch [::modal-events/modal-push modal])))

(defn show-export-sync-info-modal []
  (let [data @(rf/subscribe [::s/custom-song-delay-for-export])]
    ;;     modal (modal-card-dialog
    ;;            {:title "Export sync data"
    ;;             :content [:div.export-sync-data-content
    ;;                       [:div.field>div.control
    ;;                        [:textarea.textarea.is-primary
    ;;                         {:id "sync-info-textarea"
    ;;                          :value data}]]]
    ;;             :footer nil})]
    ;; (rf/dispatch [::modal-events/modal-push modal])
    (show-export-text-info-modal
     {:title "Export sync data"
      :text data})))
