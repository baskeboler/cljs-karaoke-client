(ns cljs-karaoke.components.menus
  (:require [stylefy.core :as stylefy]
            [re-frame.core :as rf]
            [cljs-karaoke.remote-control :as remote-control]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.modals :as modals]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.playback :refer [play stop pause update-playback-rate]]
            [cljs-karaoke.subs :as s]))

;; (def d! rf/dispatch)

(declare playlist-next next-random show-lyrics)

(defn increase-rate [] (update-playback-rate 0.1))

(defn decrease-rate [] (update-playback-rate -0.1))

(defprotocol PMenuItem
  (menu-item [this]))

(defrecord LinkMenuItem [label url]
  PMenuItem
  (menu-item [this]
    [:li>a {:href url}
     label]))

(defrecord EventMenuItem [label event]
  PMenuItem
  (menu-item [this]
    [:li>a {:on-click #(rf/dispatch event)}
     label]))

(defrecord FnMenuItem [label on-click]
  PMenuItem
  (menu-item [this]
    [:li>a {:on-click on-click}
     label]))

(def playback-items
  [(map->FnMenuItem {:label "current song information"
                     :on-click modals/show-song-info-table-modal})
   (map->FnMenuItem {:label    "Play"
                     :on-click play})
   (map->FnMenuItem {:label    "Stop"
                     :on-click stop})
   (map->FnMenuItem {:label    "Pause"
                     :on-click pause})
   (map->FnMenuItem {:label    "Increase playback rate"
                     :on-click increase-rate})
   (map->FnMenuItem {:label    "Decrease playback rate"
                     :on-click decrease-rate})
   (map->EventMenuItem {:label "Next Random Song"
                        :event [::song-events/trigger-load-random-song]})])
(def remote-control-items
  [(map->FnMenuItem {:label    "show local ID"
                     :on-click identity})
   (map->FnMenuItem {:label "signal remote karaoke"
                     :on-click identity})])

(def lyrics-items
  [(map->FnMenuItem {:label    "export song delays"
                     :on-click modals/show-export-sync-info-modal})
   (map->FnMenuItem {:label    "save custom delays"
                     :on-click (fn []
                                 (let [selected (rf/subscribe [::s/current-song])
                                       delay    (rf/subscribe [::s/lyrics-delay])]
                                   (when-not (nil? @selected)
                                     (rf/dispatch [::events/set-custom-song-delay @selected @delay]))))})
   (map->FnMenuItem {:label "Adjust lyrics delay"
                     :on-click modals/show-delay-select-modal})])

(defn menu-component []
  [:aside.menu
   [:p.menu-label "Playback"]
   [:ul.menu-list
    (doall
     (for [item playback-items]
       ^{:key (str "playback-item-" (:label item))}
       [menu-item item]))]
   [:p.menu-label "Remote Control"]
   [:ul.menu-list
    (doall
     (for [item remote-control-items]
       ^{:key (str "remote-control-item-" (:label item))}
       [menu-item item]))]
   [:p.menu-label "Lyrics"]
   [:ul.menu-list
    (doall
     (for [item lyrics-items]
       ^{:key (str "lyrics-item-" (:label item))}
       [menu-item item]))]])

(def menu-items
  {:playback       {:label "Playback"
                    :links {:play          {:label    "play"
                                            :on-click play}
                            :stop          {:label    "stop"
                                            :on-click stop}
                            :pause         {}
                            :increase-rate {}
                            :decrease-rate {}
                            :playlist-next {}
                            :next-random   {}
                            :show-lyrics   {}}}
   :audio          {:label "Audio"
                    :links {:toggle-audio-input {}}}
   :remote-control {:start-remote-control-listener {}
                    :send-remote-control-signal    {}}
   :lyrics         {:export-delays        {}
                    :persist-saved-delays {}}})

(defn menu-group [{:keys [label links]}]
  [:div (stylefy/use-style {:display :contents})
   [:p.menu-label label]
   [:ul.menu-list
    (for [[k l] (vec links)
          :let  [{:keys [label on-click]} l]]
      ^{:key (str "link__" label)}
      [:li>a {:href     "#"
              :on-click (on-click)}
       label])]])
