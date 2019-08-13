(ns cljs-karaoke.text
  (:require [bardo.transition :refer [transition]]
            [bardo.ease :refer [interpolator wrap clamp shift
                                partition-range shift-parts flip
                                reflect quad cubic poly sine
                                circle exp elastic back bounce ease]]
            [bardo.interpolate :refer [interpolate wrap-nil
                                       wrap-infinite juxt-args
                                       symmetrical-error
                                       pair-pred wrap-errors wrap-size
                                       into-lazy-seq mix blend
                                       chain pipeline]]
            [clojure.string :as string :refer [split]]))

(defn split-into-word-spans [text]
  (->> (string/split text #" ")
       (map (fn [word]
              [:span.word word]))
       (into [:span.words])))

(defn split-into-char-spans [text delta from to]
  (->>
   (for [[i c] (mapv #(vec [%1 %2]) (range) text)
         :let [delay (* i delta)]]
     ^{:keys (str "char-" i)} [:span.char (str c)])
   (into [:span.chars])))
