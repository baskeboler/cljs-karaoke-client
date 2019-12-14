(ns cljs-karaoke.events.backgrounds
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-karaoke.search :as search]
            [cljs-karaoke.events.common :as common-events]
            [ajax.core :as ajax]
            [day8.re-frame.async-flow-fx]))

(defonce fetch-bg-from-web-enabled? true)
(def ^export wallpapers
  ["wp1.jpg"
   "Dolphin.jpg"
   "wp2.jpg"
   "wp3.jpg"
   "wp4.jpg"])

(defn rand-wallpaper []
  (->> wallpapers
       (shuffle)
       (first)
       (str "/images/")))

(defn update-song-bg-flow []
  {
   ;; :first-dispatch [::update-bg-image song-name]
   :rules [{:when :seen-all-of?
            :events [::search-images
                     ::handle-fetch-bg-success
                     ::cache-song-bg-complete
                     ::set-bg-image
                     ::generate-bg-css-complete]
            :dispatch [::update-bg-image-complete]}
           {:when :seen-all-of?
            :events [::set-random-bg-image
                     ::set-bg-image
                     ::generate-bg-css-complete]
            :dispatch [::update-bg-image-complete]}
           {:when :seen-all-of?
            :events [::set-cached-bg
                     ::generate-bg-css-complete
                     ::set-bg-image
                     ::generate-bg-css-complete]
            :dispatch [::update-bg-image-complete]}
           {:when :seen-any-of?
            :events [::set-cached-bg
                     ::generate-bg-css 
                     ::handle-fetch-bg-failure
                     ::cache-song-bg-image]}
           {:when :seen?
            :events ::update-bg-image-complete
            :dispatch [::update-bg-image-flow-complete]
            :halt? true}]})

(rf/reg-event-fx
 ::search-images
 (rf/after
  (fn [db [_ q & opts]]
    (. js/console (log "Searching for bg images"))))
 (fn-traced
  [{:keys [db]} [_ q callback-event error-event]]
  {:db db
   :http-xhrio {:method :get
                :timeout 8000
                :uri (str search/base-url
                          "?cx="  search/ctx-id
                          "&key=" search/api-key
                          "&q=" q)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success callback-event
                :on-failure error-event}}))
(rf/reg-event-fx
 ::init-update-bg-image-flow
 ;; ::update-bg-image
 (fn-traced
  [{:keys [db]} [_ title]]
  (let [cached             (get-in db [:song-backgrounds title] nil)
        search-bg-enabled? (get-in db [:fetch-bg-from-web-enabled?])]
    (merge
     {:db         db
      :async-flow (update-song-bg-flow)}
     (cond
       (not (nil? cached)) {:dispatch [::set-cached-bg cached]}
       search-bg-enabled?  {:dispatch [::search-images title [::handle-fetch-bg-success title] ::handle-fetch-bg-failure]}
       :else               {:dispatch [::set-random-bg-image]})))))

(rf/reg-event-fx
 ::set-cached-bg
 (rf/after
  (fn[db _]
    (. js/console (log "Setting bg image from cache"))))

 (fn-traced
  [{:keys [db]} [_ cached]]
  {:db db
   :dispatch-n [[::set-bg-image cached]
                [::generate-bg-css cached]]}))
(rf/reg-event-fx
 ::set-random-bg-image
 (fn-traced
  [{:keys [db]} _]
  (let [url (rand-wallpaper)]
    {:db db
     :dispatch-n [[::set-bg-image url]
                  [::generate-bg-css url]]})))

(rf/reg-event-db
 ::set-bg-image
 (fn-traced
  [db [_ bg-image-url]]
  (-> db (assoc :bg-image bg-image-url))))

(rf/reg-event-fx
 ::handle-fetch-bg-success
 (fn-traced
  [{:keys [db]} [_ title res]]
  (let [candidate-image (search/extract-candidate-image res)]
    {:db         db
     :dispatch-n [(when-not (nil? candidate-image) [::set-bg-image (:url candidate-image)])
                  (when-not (nil? candidate-image) [::generate-bg-css (:url candidate-image)])
                  (when-not (nil? candidate-image) [::cache-song-bg-image title (:url candidate-image)])
                  (when (nil? candidate-image) [::set-random-bg-image])]})))

(rf/reg-event-db
 ::set-fetch-bg-from-web
 (fn-traced
  [db [_ value]]
  (-> db
      (assoc :fetch-bg-from-web-enabled? false))))

(rf/reg-event-fx
 ::handle-fetch-bg-failure
 (fn-traced
  [{:keys [db]} [_ err]]
  (. js/console (log "Failed to fetch bg from web" err))
  {:db db
   :dispatch-n [
                [::set-fetch-bg-from-web false]
                [::set-random-bg-image]]}))

(rf/reg-event-fx
 ::generate-bg-css
 (fn-traced
  [{:keys [db]} [_ url]]
  (let [styles {:background-image (str "url(\"" url "\")")
                :background-size :cover}]
    {:db (-> db (assoc :bg-style styles))
     :dispatch [::generate-bg-css-complete]})))

(rf/reg-event-fx
 ::generate-bg-css-complete
 (rf/after
  (fn [db _]
    (. js/console (log "Generate bg css complete"))))
 (fn-traced [{:keys [db]} _] {:db db}))

(rf/reg-event-fx
 ::cache-song-bg-image
 (fn-traced
  [{:keys [db]} [_ song-name image-url]]
  (let [new-db (-> db (assoc-in [:song-backgrounds song-name] image-url))]
    {:db new-db 
     :dispatch [::common-events/save-to-localstorage
                "song-bg-cache"
                (:song-backgrounds new-db)
                ::cache-song-bg-complete]})))

(rf/reg-event-fx
 ::cache-song-bg-complete
 (rf/after
  (fn [db _]
    (. js/console (log "Cache bg image complete"))))
 (fn-traced [{:keys [db]} _] {:db db}))

(rf/reg-event-fx
 ::update-bg-image-complete
 (rf/after
  (fn [db _]
    (. js/console (log "Update bg image complete"))))
 (fn-traced [{:keys [db]} _] {:db db}))

(rf/reg-event-fx
 ::update-bg-image-flow-complete
 (rf/after
  (fn [db _]
    (. js/console (log "Update bg image flow complete"))))
 (fn-traced [{:keys [db]} _] {:db db}))

;; (rf/reg-event-fx
;;  ::init-update-bg-image-flow
;;  (rf/after
;;   (fn [db [_ song-name]]
;;     (. js/console (log "Starting update bg image flow for song: " song-name))))
;;  (fn-traced
;;   [{:keys [db]} [_ song-name]]
;;   {:db db
;;    :async-flow (update-song-bg-flow song-name)}))

