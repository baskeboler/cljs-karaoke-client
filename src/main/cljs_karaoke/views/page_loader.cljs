(ns cljs-karaoke.views.page-loader
  (:require [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.subs :as subscriptions]
            [stylefy.core :as stylefy]
            [cljs-karaoke.embed :as embed :include-macros true])
  (:require-macros [cljs-karaoke.embed :refer [inline-svg]]))


(def loader-logo
  {:display :block
   :position :absolute
   :height "60vh"
   :width "60vw"
   :top "50%"
   :left "50%"
   :transform "translate(-50%, -50%)"})

(defn logo-animation []
  (reagent.core/create-class
   {:component-did-mount
    (fn [this]
      (let [s (. js/document (getElementById "logo-obj"))
            l (. s (addEventListener
                    "load"
                    (fn []
                      (let [svg (.-contentDocument s)
                            txt (. svg (getElementById "logo-text"))]
                        (. txt (setAttribute "opacity" 0))
                        (js/console.log "Loaded doc " txt)))))]
        (println "Got element " s)))

    :render (fn [this]
              [:div.my-inlined-svg
               [:object
                (stylefy/use-style
                 loader-logo
                 {:id "logo-obj"
                  :data "images/logo-2.svg"
                  :type "image/svg+xml"})]])}))

(defn page-loader-component []
  (reagent.core/create-class
   {:render
    (fn []
      [:div.pageloader
       {:class (if @(rf/subscribe [::subscriptions/pageloader-active?])
                 ["is-active"]
                 [])}
       ;; [:img (stylefy/use-style
       ;; loader-logo
       [logo-animation]])}))
       ;; {:src "/images/logo-2.svg"
       ;; :alt ""}
