(ns cljs-karaoke.components.dropdown
  (:require [reagent.core :as r :refer [atom]]
            [stylefy.core :as stylefy]
            [re-frame.core :as rf]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.modals :as modals]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.playback :refer [play stop pause update-playback-rate]]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.protocols :as protocols]))

(defprotocol DropdownItem
  (render-dropdown-item [this]))

(defrecord UrlDropdownItem [label url]
  protocols/Renderable
  (render-component [this]
    [:a.dropdown-item
     {:href url
      :on-click #(.stopPropagation %)}
     label]))

(defn  ^:export url-item [label url] (->UrlDropdownItem label url))

(defrecord FnDropdownItem [label trigger-fn]
  protocols/Renderable
  (render-component [this]
    [:a.dropdown-item
     {:on-click trigger-fn}
     label]))

(defn ^:export fn-item [label func] (->FnDropdownItem label func))
(defn ^:export divider [] [:hr.dropdown-divider])

(defrecord DropdownMenu [items]
  protocols/Renderable
  (render-component [this]
    [:div.dropdown-menu
     {:role :menu}
     (into [:div.dropdown-content]
           (for [i (:items this)]
             [protocols/render-component i]))]))

(defn dropdown-menu [items] (->DropdownMenu items))

(defrecord DropdownMenuButton [btn-label menu]
  protocols/Renderable
  (render-component [this]
    (let [active? (atom false)
          blur-fn (fn [evt]
                    (js/console.log "blur function" evt)
                    (.preventDefault evt)
                    (swap! active? not))]
      (fn []
        [:div.dropdown
         {:class (if @active? "is-active")}
         [:div.dropdown-trigger
          [:button.button
           {:aria-haspopup :true
            :on-blur blur-fn
            :on-click #(swap! active? not)}
           btn-label]]
         (protocols/render-component (:menu this))]))))

(defn ^:export dropdown-menu-button [label items]
  (->DropdownMenuButton
   label
   (dropdown-menu items)))

(extend-protocol protocols/Renderable
  cljs.core/PersistentVector
  (protocols/render-component [this] this))
    
