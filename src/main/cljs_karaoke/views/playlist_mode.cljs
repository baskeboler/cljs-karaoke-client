(ns cljs-karaoke.views.playlist-mode
  (:require [re-frame.core :as rf]
            [cljs-karaoke.playlists :as playlists]
            [cljs-karaoke.events :as events]
            [cljs-karaoke.events.songs :as song-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.songs :as songs]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.events.views :as view-events]
            [cljs-karaoke.views.navbar :refer [navbar-component]]
            [cljs-karaoke.subs :as subs]
            [cljs-karaoke.utils :as utils :refer [icon-button]]
            [cljs-karaoke.notifications :as notifications]
            [cljs-karaoke.protocols :as protocols]
            [cljs-karaoke.styles :refer [default-page-styles]]
            [stylefy.core :as stylefy]
            [goog.string :as gstr]
            [cljs-karaoke.router.core :as router]))
(defn playlist-controls [i song]
  [:div.playlist-controls.field.has-addons])

(defn playlist-action-button
  [{:keys [label icon button-classes attrs]}]
  [:button.button.is-small
   (merge
    {:class      button-classes
     :title      label
     :aria-label label}
    attrs)
   [:span.icon>i.fas.fa-fw {:class icon}]
   [:span label]])

(defn load-song-btn [pos]
  [playlist-action-button
   {:label "Queue"
    :icon "fa-folder-open"
    :button-classes "is-light"
    :attrs {:on-click #(rf/dispatch [::playlist-events/jump-to-playlist-position pos])}}])

(defn play-song-btn [pos]
  [:a.button.is-small.is-primary
   {:href (router/url-for :song :song-name (-> @(rf/subscribe [::subs/playlist]) :songs (nth pos) gstr/urlEncode))
    :title "Play this song now"
    :aria-label "Play this song now"
    :on-click #(do
                 (rf/dispatch [::playlist-events/jump-to-playlist-position pos]))}
                 ;; (rf/dispatch [::view-events/view-action-transition :go-to-playback]))}
   [:span.icon>i.fas.fa-fw.fa-play]
   [:span "Play now"]])

(defn move-song-up-btn [pos]
  [playlist-action-button
   {:label "Move up"
    :icon "fa-arrow-up"
    :button-classes "is-white"
    :attrs {:on-click #(rf/dispatch [::playlist-events/move-song-up pos])}}])


(defn move-song-down-btn [pos]
  [playlist-action-button
   {:label "Move down"
    :icon "fa-arrow-down"
    :button-classes "is-white"
    :attrs {:on-click #(rf/dispatch [::playlist-events/move-song-down pos])}}])


(defn forget-delay-btn [ song-name]
  [playlist-action-button
   {:label "Forget delay"
    :icon "fa-trash"
    :button-classes "is-danger is-light"
    :attrs {:on-click #(rf/dispatch [::song-events/forget-custom-song-delay song-name])}}])

(defn playlist-component []
  (let [pl (rf/subscribe  [::subs/playlist])
        current (rf/subscribe [::subs/current-song])]
    ;; (when-not (number? (protocols/current @pl))
      ;; (rf/dispatch-sync [::playlist-events/set-current-playlist-song])
    (if @pl
      [:div.playlist-list
       (doall
        (for [[i s] (mapv vector (map inc (range)) (protocols/songs @pl))
              :let [{:keys [title artist]} (songs/song-display-data s)]]
          ^{:key (str "playlist-row-" i)}
           [:article.playlist-item
            {:class (if (= @current s) "is-current" "")}
            [:div.playlist-item-main
            [:div.playlist-item-order (gstr/format "%02d" i)]
            [:div.playlist-item-copy
             [:div.playlist-item-heading
              [:p.playlist-item-title title]
              (when (= @current s)
                [:span.tag.is-primary.is-light "Current"])]
             (when artist
               [:p.playlist-item-artist artist])
             [:p.playlist-item-raw s]]]
           [:div.playlist-item-actions
            [load-song-btn (dec i)]
            [move-song-up-btn (dec i)]
            [move-song-down-btn (dec i)]
            [play-song-btn (dec i)]
            [forget-delay-btn s]]]))]
      [:div.playlist-unavailable.is-fullwidth
       [:h3 "Playlist is unavailable"]])))

(defn ^:export playlist-view-component []
  [:div.playlist-view.container-fluid.slide-in-bck-center>div.columns
   (stylefy/use-style default-page-styles)
   [:div.column
    [:p.title "Playlist Mode"]
    [:p.playlist-view-intro "Organize the queue, promote the next singer, and jump straight into playback."]
    [playlist-component]]])
