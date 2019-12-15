(ns cljs-karaoke.views.lyrics
  (:require [clojure.string :as str]
            [stylefy.core :as stylefy]
            [re-frame.core :as rf]
            [cljs-karaoke.lyrics :as l]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.protocols :as p]))
(defn- clean-text [t]
  (-> t
      (str/replace #"/" "")
      (str/replace #"\\" "")))
(defn- leading? [t]
  (or (str/starts-with? t "/")
      (str/starts-with? t "\\")))

(defn leading-icon []
  [:span.icon (stylefy/use-style {:margin "0 0.5em"})
   [:i.fas.fa-music.fa-fw]])

(defn frame-text [frame]
  (let [
        ;; frame        (rf/subscribe [::subs/current-frame])
        delay        (rf/subscribe [::subs/lyrics-delay])
        current-time (rf/subscribe [::subs/player-current-time])]
    ;; (fn []
      [:div.frame-text
       (doall
        (for [e    (vec (:events frame))
              :let [span-text (clean-text (p/get-text e))
                    fr-offset (:offset frame)]]
          [:span {:key   (str "evt_" (:id e))
                  :class (if (p/played? e (+ (* -1 @delay)
                                             (* 1000 @current-time)
                                             (* -1 fr-offset)))
                           ["highlighted"]
                           "")}
           (when (leading? (p/get-text e))
             [leading-icon])
           span-text]))]))
