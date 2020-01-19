(ns cljs-karaoke.components.delay-select
  (:require [re-frame.core :as rf]))

(defn ^:export delay-select-component []
  (let [delay (rf/subscribe [:cljs-karaoke.subs/lyrics-delay])]
    [:div.field
     [:div.control
      [:div.select.is-primary.is-fullwidth.delay-select
       [:select {:value     @delay
                 :on-change #(rf/dispatch [:cljs-karaoke.events/set-lyrics-delay
                                           (-> % .-target .-value (long))])}
        (for [v (vec (range -10000 10001 250))]
          [:option {:key   (str "opt_" v)
                    :value v}
           v])]]]]))
