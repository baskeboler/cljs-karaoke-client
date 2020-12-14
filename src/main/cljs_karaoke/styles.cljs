(ns cljs-karaoke.styles
  (:require [stylefy.core :as stylefy]))

(def ^:export wallpapers
  ["fancy-pants.jpg"
   "wp1.jpg"
   "Dolphin.jpg"
   "wp2.jpg"
   "wp3.jpg"
   "wp4.jpg"])

(defn random-kenburn []
  (str "kenburns-"
       (rand-nth ["top" "left" "right" "bottom" "top-right" "top-left"])))

(defn ^:export parent-style []
  ;; {:transition       "background-image 1s ease-out"
  {:background-size  "auto"
   :background-image (str "url(\"/images/" (first wallpapers) "\")")})
   ;; :animation        (str (random-kenburn)
                          ;; " 5s ease-in-out both")})

(def ^:export centered
  {:position  :fixed
   :display   :block
   :top       "50%"
   :left      "50%"
   :transform "translate(-50%, -50%)"})
(def ^:export screen-centered
  {:position  :fixed
   :display   :block
   :top       "50vh"
   :left      "50vw"
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
   :border-radius    ".5em"
   :padding          "0.5em"
   :background-color "rgba(0,0,0, 0.3)"
   :bottom           "0"
   :z-index          2
   :left             "50%"
   :transform        "translate(-50%)"
   ::stylefy/media   {{:min-width "320px"}  {:animation "slide-in-bottom 0.5s cubic-bezier(0.250, 0.460, 0.450, 0.940) both"}
                      {:min-width "1024px"} {:left      "50%"
                                             :bottom    "unset"
                                             :margin    "0.5em"
                                             :animation "slide-in-top 0.5s cubic-bezier(0.250, 0.460, 0.450, 0.940) both"
                                             :top       0}}})

(def ^:export logo-bg-style
  {:position   :fixed
   :top        "50%"
   :left       "50%"
   :max-width  "80vw"
   :max-height "80vh"
   :transform  "translate(-50%,-50%)"
   :opacity    0.5})

(def ^:export default-page-styles
  {:background-color "rgba(255,255,255,0.7)"
   :padding          "2em 1em"
   :border-radius    "0.5em"})


(def ^:export shadow-style
  {:box-shadow "0px 5px 12px 4px #797878"})
