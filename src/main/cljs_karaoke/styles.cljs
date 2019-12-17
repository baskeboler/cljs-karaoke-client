(ns cljs-karaoke.styles
  (:require [stylefy.core :as stylefy]))

(def ^:export wallpapers
  ["wp1.jpg"
   "Dolphin.jpg"
   "wp2.jpg"
   "wp3.jpg"
   "wp4.jpg"])


(defn random-kenburn []
  (str "kenburns-"
       (rand-nth ["top" "left" "right" "bottom" "top-right" "top-left"])))

(defn ^:export parent-style []
  ;; {:transition       "background-image 1s ease-out"
  {:background-size  "cover"
   :background-image (str "url(\"images/" (first wallpapers) "\")")
   :animation        (str (random-kenburn)
                          " 5s ease-in-out both")})

(def ^:export centered
  {:position  :fixed
   :display   :block
   :top       "50%"
   :left      "50%"
   :transform "translate(-50%, -50%)"})
(def ^:export top-left
  {:position :fixed
   :display  :block
   :top      0
   :left     0
   :margin   "2em 2em"})

(def ^:export top-right
  {:position :absolute
   :top      "0.5em"
   :right    "0.5em"})

(def ^:export time-display-style
  {:position         :fixed
   :display          :block
   :color            :white
   :font-weight      :bold
   :top              0
   :margin           "0.1em"
   :border-radius    ".5em"
   :padding          "0.5em"
   :background-color "rgba(0,0,0, 0.3)"
   :animation "slide-in-top 0.5s cubic-bezier(0.250, 0.460, 0.450, 0.940) both"
   ::stylefy/media   {{:min-width "320px"} {:left        0
                                            :margin-left "0.1em"}
                      {:min-width "640px"} {:left      "50%"
                                            :margin    " 1em"
                                            :transform "translate(-50%)"}}})


(def ^:export logo-bg-style
  {:position   :fixed
   :top        "50%"
   :left       "50%"
   :max-width  "80vw"
   :max-height "80vh"
   :transform  "translate(-50%,-50%)"
   :opacity    0.5})
