(ns cljs-karaoke.songs
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [clojure.string :as str]
            [cljs-karaoke.events.common]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.audio :as aud]
            [goog.string :as gstr]
            [cljs-karaoke.router :as router]
            [cljs-karaoke.lyrics :refer [preprocess-frames]]
            [cljs.core.async :as async :refer [<! >! chan go go-loop]]
            [cljs-karaoke.protocols :refer [handle-route]]))

(defn song-title [name]
  (-> name
      (str/replace #"_" " ")))

(defn song-table-pagination []
  (let [song-list    (rf/subscribe [::s/available-songs])
        current-page (rf/subscribe [::s/song-list-current-page])
        page-size    (rf/subscribe [::s/song-list-page-size])
        filter-text  (rf/subscribe [::s/song-list-filter])
        page-offset  (rf/subscribe [::s/song-list-offset])
        next-fn      #(rf/dispatch [::song-list-events/set-song-list-current-page (inc @current-page)])
        prev-fn      #(rf/dispatch [::song-list-events/set-song-list-current-page (dec @current-page)])]
    (fn []
      (when-let [song-count (count @song-list)]
        [:nav.pagination {:role :navigation}
         [:a.pagination-previous {:on-click #(when (pos? @current-page) (prev-fn))
                                  :disabled (if-not (pos? @current-page) true false)}
          "Previous"]
         [:a.pagination-next {:on-click #(when (> (- song-count @page-offset)
                                                  @page-size)
                                           (next-fn))
                              :disabled (if-not (> (- song-count @page-offset)
                                                   @page-size)
                                          true
                                          false)}
          "Next"]]))))
(defn song-filter-component []
  (let [filt (rf/subscribe [::s/song-list-filter])]
    [:div.field>div.control.has-icons-left
     [:span.icon
      [:i.fas.fa-search]]
     [:input.input.is-primary
      {:value     @filt
       :name      "filter-input"
       :on-change #(rf/dispatch [::song-list-events/set-song-filter
                                 (-> % .-target .-value)])}]]))

(defn song-url [song-name]
  (if @(rf/subscribe [::s/has-delay? song-name])
    (router/url-for :song-with-offset :song-name (gstr/urlEncode song-name) :offset @(rf/subscribe [::s/song-delay song-name]))
    (router/url-for :song :song-name (gstr/urlEncode song-name))))

(defn song-table-component
  []
  (let [current-page            (rf/subscribe [::s/song-list-current-page])
        page-size               (rf/subscribe [::s/song-list-page-size])
        filter-text             (rf/subscribe [::s/song-list-filter])
        page-offset             (rf/subscribe [::s/song-list-offset])]
    [:div.card.song-table-component.flip-in-hor-bottom
     [:div.card-header]
     [:div.card-content
      [song-filter-component]
      [song-table-pagination]
      [:table.table.is-narrow.is-fullwidth.song-table
       [:thead
        [:tr
         [:th "Song"]
         [:th]]]
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
                  {:href (song-url name)} ;(str "#/songs/" name)}
                  [:i.fas.fa-play]]]]))]]

      [song-table-pagination]]]))

(defn load-song
  ([name]
   (rf/dispatch [::song-events/load-song-if-initialized  name]))
  ([]
   (when-let [song @(rf/subscribe [::s/playlist-current])]
     (load-song song))))

(defmethod handle-route :song
  [{:keys [params]}]
  (load-song (:song-name params))
  :playback)

(defmethod handle-route :song-with-offset
  [{:keys [params]}]
  (let [{:keys [song-name offset]} params]
    (load-song song-name)
    (rf/dispatch [::events/set-custom-song-delay song-name (js/parseInt offset)])
    (rf/dispatch [::events/set-lyrics-delay (js/parseInt offset)])
    :playback))
