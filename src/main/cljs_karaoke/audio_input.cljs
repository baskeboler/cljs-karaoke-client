(ns cljs-karaoke.audio-input
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent :refer [atom]]
            [cljs-karaoke.subs.audio :as audio-subs]
            [cljs-karaoke.notifications :refer [add-notification notification]]
            [cljs.core.async :as async :refer [timeout <! >! go-loop]]))


(defn ^export audio-viz []
  (when-let  [freq-data (rf/subscribe [::audio-subs/freq-data])]
    (when-not (empty? @freq-data)
      [:div.audio-spectrum
       (doall
        (for [[i v] (mapv (fn [a b] [b a])  @freq-data (map inc (range)))]
          ^{:key (str "bar-" i)}
          [:span.freq-bar
           {:style {:height v}}]))])))
                 
      

