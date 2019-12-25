(ns cljs-karaoke.views.lyrics
  (:require [clojure.string :as str]
            [goog.string :as gstr]
            [stylefy.core :as stylefy]
            [re-frame.core :as rf]
            [cljs-karaoke.lyrics :as l]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.protocols :as p]))

(defn- clean-text
  [t]
  (-> t
      (str/replace #"/" "")
      (str/replace #"\\" "")
      (str/replace #" " "&#160;")
      (gstr/unescapeEntities)))

(defn- leading? [t]
  (or (str/starts-with? t "/")
      (str/starts-with? t "\\")))

(defn leading-icon []
  [:span.icon (stylefy/use-style {:margin "0 0.5em"})
   [:i.fas.fa-music.fa-fw]])

(defn- neg [n]
  (* -1 n))
(defn- s->ms [s] (* 1000 s))

(defn frame-text [frame]
  (let [delay        (rf/subscribe [::subs/lyrics-delay])
        current-time (rf/subscribe [::subs/player-current-time])]
      [:div.frame-text
       {:key (str "frame-" (:id frame))}
       (doall
        (for [e    (vec (:events frame))
              :let [span-text (clean-text (p/get-text e))
                    fr-offset (:offset frame)
                    time-adjusted (+ (neg @delay) (s->ms @current-time) (neg fr-offset))]]
          [:span {:key   (str "evt_" (:id e))
                  :class (if (p/played? e time-adjusted)
                           ["highlighted"]
                           nil)}
           (when (leading? (p/get-text e))
             [leading-icon])
           span-text]))]))
