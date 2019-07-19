(ns cljs-karaoke.views.lyrics
  (:require [clojure.string :as str]
            [stylefy.core :as stylefy]))


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
  [:div.frame-text
   (for [e (vec (:events frame))
         :let [span-text (clean-text (:text e))]]
     [:span {:key (str "evt_" (:id e))
             :class (if (:highlighted? e) ["highlighted"] [])}
      (when (leading? (:text e)) [leading-icon]) span-text])])
