(ns cljs-karaoke.songs
  (:require [reagent.core :as reagent :refer [atom]]
            [re-frame.core :as rf :include-macros true]
            [clojure.string :as str]
            [cljs-karaoke.events.common]
            [cljs-karaoke.subs :as s]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.views :as view-events]
            [cljs-karaoke.audio :as aud]
            [goog.string :as gstr]
            [cljs-karaoke.router.core :as router]
            [cljs-karaoke.lyrics :refer [preprocess-frames]]
            [cljs.core.async :as async :refer [<! >! chan go go-loop]]
            [cljs-karaoke.protocols :refer [handle-route ViewDispatcher dispatch-view]]
            [pushy.core :as pushy]))
(declare song-url)

(defn clean-song-name [name]
  (-> name
      (str/replace #"_" " ")
      (str/replace #"\s+" " ")
      (str/trim)))

(defn song-display-data [name]
  (let [clean-name     (clean-song-name name)
        [artist title] (str/split clean-name #"-" 2)]
    (if (str/blank? title)
      {:title clean-name
       :artist nil}
      {:title  (str/trim title)
       :artist (str/trim artist)})))

(defn song-title [name]
  (:title (song-display-data name)))

(defn filtered-song-names [available-songs filter-text]
  (->> available-songs
       (filter #(str/includes?
                 (str/lower-case %)
                 (str/lower-case filter-text)))
       sort
       vec))

(defn song-results-summary []
  (let [song-list    (rf/subscribe [::s/available-songs])
        page-size    (rf/subscribe [::s/song-list-page-size])
        filter-text  (rf/subscribe [::s/song-list-filter])
        page-offset  (rf/subscribe [::s/song-list-offset])
        current-page (rf/subscribe [::s/song-list-current-page])]
    (fn []
      (let [filtered-songs (filtered-song-names @song-list @filter-text)
            total-count    (count filtered-songs)
            start-index    (if (pos? total-count) (inc @page-offset) 0)
            end-index      (min total-count (+ @page-offset @page-size))
            page-count     (max 1 (int (js/Math.ceil (/ total-count @page-size))))]
        [:div.song-results-summary
         [:p
          (if (pos? total-count)
            (str "Showing " start-index "-" end-index " of " total-count " songs")
            "No songs match this search yet.")]
         [:p.song-results-summary-page
          (str "Page " (inc @current-page) " of " page-count)]]))))

(defn song-table-pagination []
  (let [song-list    (rf/subscribe [::s/available-songs])
        current-page (rf/subscribe [::s/song-list-current-page])
        page-size    (rf/subscribe [::s/song-list-page-size])
        filter-text  (rf/subscribe [::s/song-list-filter])
        page-offset  (rf/subscribe [::s/song-list-offset])
        next-fn      #(rf/dispatch [::song-list-events/set-song-list-current-page (inc @current-page)])
        prev-fn      #(rf/dispatch [::song-list-events/set-song-list-current-page (dec @current-page)])]
    (fn []
      (let [song-count (count (filtered-song-names @song-list @filter-text))]
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
       :placeholder "Search by artist, title, or filename"
       :aria-label "Search songs"
       :autocomplete "off"
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
     [:div.card-content
      [song-filter-component]
      [song-results-summary]
      [song-table-pagination]
      [:div.song-library-list
       (doall
        (for [name (->> (filtered-song-names @(rf/subscribe [::s/available-songs]) @filter-text)
                        (drop @page-offset)
                        (take @page-size)
                        (into []))
              :let [{:keys [title artist]} (song-display-data name)]]
          ^{:key name}
          [:article.song-library-item
           [:div.song-library-item-copy
            [:div.song-library-item-heading
             [:div
              [:p.song-library-item-title title]
              (when-not (str/blank? artist)
                [:p.song-library-item-artist artist])]
             (when @(rf/subscribe [::s/has-delay? name])
               [:span.tag.is-link.is-light.song-library-item-tag "Saved sync"])]]
           [:div.song-library-item-actions
            [:a.button.is-primary.is-light
             {:href (song-url name)
              :title (str "Play " title)
              :aria-label (str "Play " title)}
             [:span.icon
              [:i.fas.fa-play]]
             [:span "Play"]]]]))]
      [song-table-pagination]]]))

(defn load-song
  ([name]
   (rf/dispatch [::song-events/load-song-if-initialized  name]))
  ([]
   (when-let [song @(rf/subscribe [::s/playlist-current])]
     (load-song song))))

(defmethod handle-route :song
  [{:keys [params]}]
  (load-song (-> (:song-name params)
                 (str/replace "+" " ")))

  :playback)

(defmethod handle-route :song-with-offset
  [{:keys [params]}]
  (let [{:keys [song-name offset]} params
        song-name (-> song-name
                      (str/replace "+" " "))]
    (load-song song-name)
    (rf/dispatch [::events/set-custom-song-delay song-name (js/parseInt offset)])
    (rf/dispatch [::events/set-lyrics-delay (js/parseInt offset)])
    :playback))

;; (defmethod handle-route :random-song
  ;; [_]
  ;; (let [random-song-name (rand-nth @(rf/subscribe [::s]))]))
