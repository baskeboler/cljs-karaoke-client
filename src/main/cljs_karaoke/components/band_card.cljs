(ns cljs-karaoke.components.band-card
  (:require [reagent.core :as r]
            [re-frame.core :as rf]))

(defn ^:export band-card-component
  [{:keys [description title social image]
    :or {description "No description."
         title "No title"
         social nil
         image nil}}]
  [:div.card
   (when image
     [:div.card-image
      [:figure.image.is-fullwidth
       [:img {:src image}]]])
   [:div.card-content
    [:div.title title]
    [:div.content description]]
   (when-let [{:keys [facebook instagram twitter]} social]
     [:footer.card-footer
      (when facebook
        [:a.card-footer-item {:href facebook} [:i.fab.fa-3x.fa-fw.fa-facebook]])
      (when instagram
        [:a.card-footer-item {:href instagram} [:i.fab.fa-3x.fa-fw.fa-instagram]])
      (when twitter
        [:a.card-footer-item {:href twitter} [:i.fab.fa-3x.fa-fw.fa-twitter]])])])
    
         
