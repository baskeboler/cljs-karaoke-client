(ns cljs-karaoke.events.backgrounds
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.async-flow-fx]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-karaoke.search :as search]
            [cljs-karaoke.styles :refer [wallpapers]]
            [cljs-karaoke.events.common :as common-events]
            [ajax.core :as ajax]
            [shadow.loader :as loader :refer [loaded? load with-module]]))
            ;; [cljs-karaoke.mongo :as mongo]))
(defonce fetch-bg-from-web-enabled? true)

(defn rand-wallpaper []
  (->> wallpapers
       (shuffle)
       (first)
       (str "./images/")))

(defn update-song-bg-flow []
  {;; :first-dispatch [::update-bg-image song-name]
   ;; :id    (gensym ::update-song-bg-flow)
   :rules [
           ;; {:when     :seen-all-of?
           ;; :events   [::search-images
           ;; ::handle-fetch-bg-success
           ;; ::cache-song-bg-complete
           ;; ::set-bg-image
           ;; ::generate-bg-css-complete
           ;; :dispatch [::update-bg-image-complete]
           {:when     :seen-all-of?
            :events   [
                       ::set-bg-image
                       ::generate-bg-css-complete]
            :dispatch [::update-bg-image-complete]
            :halt?    false}
           ;; {:when     :seen-all-of?
           ;; :events   [::set-cached-bg
           ;; ::set-bg-image
           ;; ::generate-bg-css-complete]
           ;; :dispatch [::update-bg-image-complete]}
           ;; {:when   :seen-any-of?
           ;; :events [::set-cached-bg
           ;; ::generate-bg-css
           ;; ::handle-fetch-bg-failure
           ;; ::cache-song-bg-image
           ;; :dispatch [::update-bg-image-complete]}
           {:when     :seen-all-of?
            :events   [::update-bg-image-complete]
            :dispatch [::update-bg-image-flow-complete]
            :halt?    true}]})

(defn- check-bg-updates
  [push-count backgrounds]
  (when (and (not (zero? push-count))
             (zero? (mod push-count 5)))
    (println "[mongo backend] pushing backgrounds")
    (cljs-karaoke.mongo/save-backgrounds backgrounds)
    (println "[mongo backend] backgrounds pushed")))

(rf/reg-event-db
 ::inc-google-search-count
 (rf/after
  (fn [db _]
    (if (loaded? "mongo")
      (check-bg-updates (:google-search-count db)
                        (:song-backgrounds db))
      (with-module "mongo"
        #(check-bg-updates (:google-search-count db)
                           (:song-backgrounds db))))))
 (fn-traced
  [db _]
  (-> db
      (update :google-search-count inc))))

(rf/reg-event-fx
 ::search-images
 (rf/after
  (fn [db [_ q & opts]]
    (. js/console (log "Searching for bg images"))))
 (fn-traced
  [{:keys [db]} [_ q callback-event error-event]]
  {:http-xhrio {:method :get
                :timeout 8000
                :uri (str search/base-url
                          "?cx="  search/ctx-id
                          "&key=" search/api-key
                          "&q=" q)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success callback-event
                :on-failure error-event}
   :dispatch [::inc-google-search-count]}))


(rf/reg-event-fx
 ::init-update-bg-image-flow
 ;; ::update-bg-image
 (fn-traced
  [{:keys [db]} [_ title]]
  (let [cached             (get-in db [:song-backgrounds title] nil)
        search-bg-enabled? (get-in db [:fetch-bg-from-web-enabled?])]
    (merge
     {:async-flow (update-song-bg-flow)}
     (cond
       (not (nil? cached)) {:dispatch [::set-cached-bg cached]}
       search-bg-enabled?  {:dispatch [::search-images title [::handle-fetch-bg-success title] [::handle-fetch-bg-failure]]}
       :else               {:dispatch [::set-random-bg-image]})))))

(rf/reg-event-fx
 ::set-cached-bg
 (rf/after
  (fn [db _]
    (. js/console (log "Setting bg image from cache"))))

 (fn-traced
  [{:keys [db]} [_ cached]]
  {:dispatch-n [[::set-bg-image cached]
                [::generate-bg-css cached]]}))
(rf/reg-event-fx
 ::set-random-bg-image
 (fn-traced
  [{:keys [db]} _]
  (let [url (rand-wallpaper)]
    {:dispatch-n [[::set-bg-image url]
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
    {:dispatch-n [(when-not (nil? candidate-image) [::set-bg-image (:url candidate-image)])
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
  (println "Failed to fetch bg from web" err)
  (let [should-disable? (>= (:status err) 400)]
    {:dispatch-n [[::set-fetch-bg-from-web (= should-disable? false)]
                  [::set-random-bg-image]]})))

(rf/reg-event-fx
 ::generate-bg-css
 (fn-traced
  [{:keys [db]} [_ url]]
  (let [styles {:background-image (str "url(\"" url "\"), url(" (rand-wallpaper) ")")
                :background-size :cover}]
                ;; :transition "all 2s ease"}]
    {:db (-> db (assoc :bg-style styles))
     :dispatch [::generate-bg-css-complete]})))

(rf/reg-event-db
 ::generate-bg-css-complete
 (fn-traced
  [db _]
  (. js/console (log "Generate bg css complete"))
  db))


(rf/reg-event-fx
 ::save-bg-cache-to-localstorage
 (fn-traced
  [{:keys [db] } [_ cb-event]]
  {:dispatch [::common-events/save-to-localstorage "song-bg-cache" (:song-backgrounds db) cb-event]}))

(rf/reg-event-fx
 ::cache-song-bg-image
 (fn-traced
  [{:keys [db]} [_ song-name image-url]]
  (let [new-db (-> db (assoc-in [:song-backgrounds song-name] image-url))]
    {:db new-db
     :dispatch [::save-bg-cache-to-localstorage ::cache-song-bg-complete]})))
     ;; :dispatch [::common-events/save-to-localstorage
                ;; "song-bg-cache"
                ;; (:song-backgrounds new-db)
                ;; ::cache-song-bg-complete]})))

(rf/reg-event-fx
 ::forget-cached-song-bg-image
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db (-> db
           (update :song-backgrounds dissoc song-name))
   :dispatch [::save-bg-cache-to-localstorage ::cache-song-bg-complete]}))
(rf/reg-event-fx
 ::cache-song-bg-complete
 ;; (rf/after
  ;; (fn [db _])
 (fn-traced
  [{:keys [db]} _]
  (. js/console (log "Cache bg image complete"))
  {}))

(rf/reg-event-fx
 ::update-bg-image-complete
 ;; (rf/after
  ;; (fn [db _])
 (fn-traced
  [{:keys [db]} _]
  (. js/console (log "Update bg image complete"))
  {}))

(rf/reg-event-fx
 ::update-bg-image-flow-complete
 ;; (rf/after
  ;; (fn [db _])
  (fn-traced
   [{:keys [db]} _]
   (. js/console (log "Update bg image flow complete"))
   {}))

;; (rf/reg-event-fx
;;  ::init-update-bg-image-flow
;;  (rf/after
;;   (fn [db [_ song-name]]
;;     (. js/console (log "Starting update bg image flow for song: " song-name))))
;;  (fn-traced
;;   [{:keys [db]} [_ song-name]]
;;   {:db db
;;    :async-flow (update-song-bg-flow song-name)}))

(rf/reg-event-fx
 ::toggle-backgrounds
 (fn-traced
  [{:keys [db]} _]
  {:db (update db :background-enabled? not)}))



(rf/reg-event-fx
 ::toggle-animated-backgrounds
 (fn-traced
  [{:keys [db]} _]
  {:db (update db :animated-background-enabled? not)}))
