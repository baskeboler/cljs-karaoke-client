(ns cljs-karaoke.audio-input
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.notifications :refer [add-notification notification]]
            [cljs.core.async :as async :refer [timeout <! >! go-loop]]
            [cljs-karaoke.events.audio :as audio-events]
            [cljs-karaoke.modals :refer [modal-card-dialog]]
            [cljs-karaoke.events.modals :as modal-events]))

(defn enable-audio-input-confirmation-modal-content []
  [:div.enable-audio-input-confirmation-content
   [:h5.is-danger "Danger"]
   [:p "Please make sure you have an external microphone connected to the mic jack, otherwise if you are using a laptop mic and the laptop speakers, the result will be unpleasant."]])

(defn enable-audio-input-confirm-dialog []
  (let [modal [modal-card-dialog {:title "Warning"
                                  :content [enable-audio-input-confirmation-modal-content]
                                  :footer [:div.footer-buttons
                                           [:button.button.is-danger
                                            {:on-click #(do
                                                          (rf/dispatch [::audio-events/init-audio-input])
                                                          (rf/dispatch [::modal-events/modal-pop]))}
                                            "Fuck it, enable mic"]
                                           [:button.button.is-primary
                                            {:on-click #(rf/dispatch [::modal-events/modal-pop])}
                                            "Get me outta here"]]}]]
    modal))

(defn ^:export audio-viz []
  (when-let  [freq-data (rf/subscribe [::audio-subs/freq-data])]
    (fn []
      (when-not (empty? @freq-data)
        [:div.audio-spectrum
         (doall
          (for [[i v] (mapv (fn [a b] [b a])  @freq-data (map inc (range)))]
            ^{:key (str "bar-" i)}
            [:span.freq-bar
             {:style {:height (str (* 100 v) "%")}}]))])))) 

(defn test-viz []
  [:div.audio-spectrum
   (for [i (take 60 (range))]
     ^{:key (str "k" i)}
     [:span.freq-bar
      {:style {:height (* 5 i)}}])])

(defn ^:export enable-audio-input-button []
  [:button.button.is-danger
   (merge
    {:on-click #(rf/dispatch [::modal-events/modal-push [enable-audio-input-confirm-dialog]])}
    (when @(rf/subscribe [::audio-subs/microphone-enabled?])
      {:disabled true}))
   [:span.icon>i.fa.fa-microphone-alt]])

(defn ^:export spectro-overlay []
  (when-let  [freq-data (rf/subscribe [::audio-subs/freq-data])]
    (fn []
      (when-not (empty? @freq-data)
       [:div.audio-spectrum-overlay
        (doall
         (for [[i v] (mapv (fn [a b] [b a])  @freq-data (map inc (range)))]
           ^{:key (str "bar-" i)}
           [:span.freq-bar
            {:style {:height (str (* 100 v) "%")}}]))]))))
