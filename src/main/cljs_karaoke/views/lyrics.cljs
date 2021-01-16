(ns cljs-karaoke.views.lyrics
  (:require [clojure.string :as str]
            [goog.string :as gstr]
            [stylefy.core :as stylefy]
            [re-frame.core :as rf]
            ;; [cljs-karaoke.lyrics :as l]
            [cljs-karaoke.subs :as subs]
            [clj-karaoke.protocols :as p]
            ;; [clj-karaoke.lyrics-event :as le]
            ;; [clj-karaoke.lyrics-frame :as lf]
            [clj-karaoke.song-data]))

(defn- clean-text
  [t]
  (-> t
      (str/replace #"/" "")
      (str/replace #"\\" "")
      (str/replace #" " "&nbsp;")
      ;; (str/replace #" " "&#160;")
      (gstr/unescapeEntities)))

(defn- leading? [e]
  (let [t (p/get-text e)]
    (or (zero? (p/get-offset e))
        (str/starts-with? t "/")
        (str/starts-with? t "\\")
        (str/starts-with? t "\n")
        (str/ends-with? t "\n"))))

(def leading-icons
  #{"fa-music" "fa-fire" "fa-bolt" "fa-skull-crossbones" "fa-meteor" "fa-radiation" "fa-skull"})

(defn leading-icon []
  [:span.icon (stylefy/use-style {:margin "0 0.5em"})
   [:i.fas.fa-fw
    {:class (->> leading-icons (into []) rand-nth)}]])

(defn- neg [n]
  (* -1 n))
(defn- s->ms [s] (* 1000 s))

(defn frame-text []
  (let [song         (rf/subscribe [::subs/playback-song])
        current-time (rf/subscribe [::subs/song-position-ms-adjusted])]
    (fn []
      (let [frame (p/get-current-frame @song @current-time)]
        (into
         [:div.frame-text]
         (doall
          (for [e    (:events frame)
                :let [span-text (clean-text (p/get-text e))
                      fr-offset (p/get-offset frame)
                      time-adjusted (+  @current-time (neg fr-offset))]]
            [:span {:key   (str "evt_" (:id e))
                    :class (if (p/played? e time-adjusted)
                             ["highlighted"]
                             nil)}
             (when (leading? e)
               [leading-icon])
             span-text])))))))
