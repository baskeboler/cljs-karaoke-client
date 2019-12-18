(ns cljs-karaoke.songs
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string :as str]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.remote-control.commands :as cmds]
            [cljs-karaoke.events.http-relay :as remote-events]
            [re-frame.core :as rf :include-macros true]
            [cljs-karaoke.audio :as aud]
            [cljs-karaoke.lyrics :refer [preprocess-frames]]
            [cljs.core.async :as async :refer [<! >! chan go go-loop]]))

(defn song-title [name]
  (-> name
      (str/replace #"_" " ")))

(defn song-table-pagination []
  (let [song-list    (rf/subscribe [::s/available-songs])
        ;; song-count   (count song-list)
        current-page (rf/subscribe [::s/song-list-current-page])
        page-size    (rf/subscribe [::s/song-list-page-size])
        filter-text  (rf/subscribe [::s/song-list-filter])
        page-offset  (rf/subscribe [::s/song-list-offset])
        next-fn      #(rf/dispatch [::song-list-events/set-song-list-current-page (inc @current-page)])
        prev-fn      #(rf/dispatch [::song-list-events/set-song-list-current-page (dec @current-page)])]
    (fn []
      (let [song-count (count @song-list)]
        [:nav.pagination {:role :navigation}
         [:a.pagination-previous {:on-click #(when (pos? @current-page) (prev-fn))
                                  :disabled (if-not (pos? @current-page) true false)}]]
        "Previous"
         [:a.pagination-next {:on-click #(when (> (- song-count @page-offset)
                                                  @page-size)
                                           (next-fn))
                              :disabled (if-not (> (- song-count @page-offset)
                                                   @page-size)
                                          true
                                          false)}]
        "Next"))))
(defn song-filter-component []
  (let [filt (rf/subscribe [::s/song-list-filter])]
    [:div.field>div.control.has-icon
     [:input.input.is-primary
      {:value     @filt
       :name      "filter-input"
       :on-change #(rf/dispatch [::song-list-events/set-song-filter
                                 (-> % .-target .-value)])}]
     [:span.icon
      [:i.fas.fa-search]]]))
(defn song-table-component
  []
  (let [current-page            (rf/subscribe [::s/song-list-current-page])
        page-size               (rf/subscribe [::s/song-list-page-size])
        filter-text             (rf/subscribe [::s/song-list-filter])
        page-offset             (rf/subscribe [::s/song-list-offset])
        remote-control-enabled? (rf/subscribe [:cljs-karaoke.subs.http-relay/remote-control-enabled?])]
    [:div.card.song-table-component
     [:div.card-header]
     [:div.card-content
      [song-filter-component]
      [song-table-pagination]
      [:table.table.is-fullwidth.song-table
       [:thead
        [:tr
         [:th "Song"]
         [:th]
         (when @remote-control-enabled?
           [:th])]]
       [:tbody
        (doall
         (for [name (->> @(rf/subscribe [::s/available-songs]) 
                         (filter #(clojure.string/includes?
                                   (clojure.string/lower-case %)
                                   (clojure.string/lower-case @filter-text)))
                         (sort)
                         (drop @page-offset)
                         (take @page-size) ;(vec (sort (keys song-map)))
                         (into []))
               :let [title (song-title name)]]
           [:tr {:key name}
            [:td title]
            [:td [:a
                  {:href (str "#/songs/" name)}
                  [:i.fas.fa-play]]]
            (when @remote-control-enabled?
              [:td
               [:a
                {:on-click (fn []
                             (let [cmd (cmds/play-song-command name)]
                               (rf/dispatch [::remote-events/remote-control-command cmd])))}
                "Play remotely"]])]))]]
      [song-table-pagination]]]))

(defn load-song
  ([name]
   (rf/dispatch-sync [::song-events/trigger-load-song-flow name]))
  ([]
   (when-let [song @(rf/subscribe [::s/playlist-current])]
     (load-song song))))
