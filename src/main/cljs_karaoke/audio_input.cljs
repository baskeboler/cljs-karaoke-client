(ns cljs-karaoke.audio-input
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.notifications :refer [add-notification notification]]
            [cljs.core.async :as async :refer [timeout <! >! go-loop]]
            [cljs-karaoke.events.audio :as audio-events]))

(defn ^export audio-viz []
  (when-let  [freq-data (rf/subscribe [::audio-subs/freq-data])]
    (when-not (empty? @freq-data)
      [:div.audio-spectrum
       (doall
        (for [[i v] (mapv (fn [a b] [b a])  @freq-data (map inc (range)))]
          ^{:key (str "bar-" i)}
          [:span.freq-bar
           {:style {:height v}}]))])))

(defn spectro-overlay []
  (when-let  [freq-data (rf/subscribe [::audio-subs/freq-data])]
    (when-not (empty? @freq-data)
      [:div.audio-spectrum-overlay
       (doall
        (for [[i v] (mapv (fn [a b] [b a])  @freq-data (map inc (range)))]
          ^{:key (str "bar-" i)}
          [:span.freq-bar
           {:style {:height (str (* 100 v) "%")}}]))])))
(defn test-viz []
  [:div.audio-spectrum
   (for [i (take 60 (range))]
     ^{:key (str "k" i)}
     [:span.freq-bar
      {:style {:height (* 5 i)}}])])

(defn ^export enable-audio-input-button []
  [:button.button.is-danger
   (merge
    {:on-click #(rf/dispatch [::audio-events/init-audio-input])}
    (when @(rf/subscribe [::audio-subs/microphone-enabled?])
      {:disabled true}))
   [:span.icon>i.fa.fa-microphone-alt]])
