(ns cljs-karaoke.events
  (:require [re-frame.core :as rf :include-macros true]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [ajax.core :as ajax]
            [cljs.core.async :as async :refer [go go-loop <! >! chan]]
            [day8.re-frame.async-flow-fx]
            [cljs.reader :as reader]
            [clojure.string :refer [replace]]
            [cljs-karaoke.lyrics :refer [preprocess-frames]]
            [cljs-karaoke.search :as search]
            [cljs-karaoke.playlists :as pl]
            [cljs-karaoke.events.common :as common-events
             :refer [reg-set-attr save-to-localstore get-from-localstorage
                     get-custom-delays-from-localstorage set-location-hash]]
            [cljs-karaoke.events.views :as views-events]
            [cljs-karaoke.events.playlists :as playlist-events]
            [cljs-karaoke.events.song-list :as song-list-events]
            [cljs-karaoke.audio :as aud]))
(defonce fetch-bg-from-web-enabled? true)
(declare save-custom-delays-to-localstore)

(defn init-flow []
  {
   ;; :first-dispatch [::init-fetches]
   :rules [{:when :seen?
            :events [::handle-fetch-background-config-complete]
            :dispatch [::init-song-bg-cache]}
           {:when :seen?
            :events [::handle-fetch-delays-complete]
            :dispatch [::init-song-delays]}
           {:when :seen?
            :events [::handle-fetch-delays-complete]
            :dispatch [::playlist-events/build-verified-playlist]}
           {:when :seen-any-of?
            :events [::handle-fetch-background-config-failure
                     ::handle-fetch-delays-failure]
            :dispatch-n [[::set-pageloader-active? false]
                         [::boot-failure]]
            :halt? true}
           {:when :seen?
            :events ::playlist-ready
            :dispatch-n [[::set-current-view :playback]
                         [::playlist-load]]}
           {:when :seen-all-of?
            :events [::song-bgs-loaded
                     ::song-delays-loaded
                     ::set-audio
                     ::set-audio-events
                     ::initial-audio-setup-complete
                     ::playlist-events/playlist-ready
                     ::views-events/views-state-ready
                     ::song-list-events/song-list-ready]
            :dispatch-n [[::set-pageloader-active? false]
                         [::initialized]]
            :halt? true}]})
(rf/reg-event-db
 ::boot-failure
 (fn [db [_ e]]
   (.log js/console "Failed to boot: " e)
   (-> db
       (assoc :boot-failed? true))))

(rf/reg-event-db
 ::initialized
 (fn-traced
  [db _]
  (. js/console (log "initialized!"))
  (-> db
      (assoc :initialized? true))))

(rf/reg-event-fx
 ::init-db
 ;; ::init-fetches
 (fn-traced [_ _]
            {:db {:current-frame nil
                  :lyrics nil
                  :lyrics-loaded? false
                  :lyrics-fetching? false
                  :lyrics-delay -1000
                  :audio nil
                  :audio-events nil
                  :display-lyrics? false
                  :current-song nil
                  :player-status nil
                  :can-play? false
                  :highlight-status nil
                  :playing? false
                  :toasty? false
                  :player-current-time 0
                  :song-duration 0
                  :custom-song-delay {}
                  :song-backgrounds {}
                  :stop-channel (chan)
                  :loop? true
                  :initialized? false
                  :base-storage-url "https://karaoke-files.uyuyuy.xyz"
                  :current-view :home
                  :pageloader-active? true
                  :display-home-button? true
         ;; :playlist (pl/build-playlist)
                  :modals []}
    ;; :dispatch-n [[::fetch-custom-delays]
                 ;; [::fetch-song-background-config]}
                 ;; [::init-song-bg-cache]]}))
             :async-flow (init-flow)

             :dispatch-n [[::fetch-custom-delays]
                          [::fetch-song-background-config]
                          [::views-events/init-views-state]
                          [::initial-audio-setup]
                          [::song-list-events/init-song-list-state]]}))

;; (rf/reg-event-fx
;;  ::init-fetches
;;  (fn-traced
;;   [{:keys [db]} _]
;;   {:db db
;;    :dispatch-n [[::fetch-custom-delays]
;;                 [::fetch-song-background-config]
;;                 [::views-events/init-views-state]
;;                 [::initial-audio-setup]
;;                 [::song-list-events/init-song-list-state]]}))

(rf/reg-event-db
 ::toggle-toasty?
 (fn-traced
  [db _]
  (-> db (update :toasty? not))))

(rf/reg-event-fx
 ::trigger-toasty
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::toggle-toasty?]
   :dispatch-later [{:ms 800 :dispatch [::toggle-toasty?]}]}))

(rf/reg-event-fx
 ::http-fetch-fail
 (fn-traced
  [db [_ err dispatch-n-vec]]
  (println "fetch failed" err)
  {:db db
   :dispatch-n dispatch-n-vec}))

(rf/reg-event-fx
 ::fetch-custom-delays
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio {:method :get
                :uri (str (get db :base-storage-url "") "/lyrics/delays.edn")
                :timeout 8000
                :response-format (ajax/text-response-format)
                :on-success [::handle-fetch-delays-success]
                :on-failure [::handle-fetch-delays-failure]}}))
(rf/reg-event-fx
 ::handle-fetch-delays-success
 (fn-traced
  [{:keys [db]} [_ delays-resp]]
  (let [r (-> delays-resp
              (reader/read-string))]
    {:db (-> db
             (update :custom-song-delay merge r))
     :dispatch [::handle-fetch-delays-complete]})))

(rf/reg-event-fx
 ::save-custom-song-delays-to-localstorage
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::common-events/save-to-localstorage "custom-song-delays" (:custom-song-delay db) nil]}))          

(rf/reg-event-fx
 ::handle-fetch-delays-complete
 (fn [{:keys [db]} _]
   {:db db
    :dispatch [::save-custom-song-delays-to-localstorage]}))

(rf/reg-event-fx
 ::fetch-song-background-config
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio {:method :get
                :uri (str (get db :base-storage-url "") "/backgrounds.edn")
                :timeout 8000
                :response-format (ajax/text-response-format)
                :on-success [::handle-fetch-background-config-success]
                :on-failure [::handle-fetch-background-config-failure]}}))
(rf/reg-event-fx
 ::handle-fetch-background-config-success
 (fn-traced
  [{:keys [db] :as cofx} [_ a]]
  (let [c (-> a
              (reader/read-string))]
    {:db (-> db
             (update :song-backgrounds merge c))
     :dispatch [::handle-fetch-background-config-complete]})))
(rf/reg-event-fx
 ::handle-fetch-background-config-failure
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::handle-fetch-background-config-complete]}))

(rf/reg-event-db
 ::handle-fetch-background-config-complete
 (fn-traced [db _] db))

(rf/reg-event-db
 ::set-location-hash
 (rf/after
  (fn [_ [_ new-hash]]
   (set-location-hash new-hash)))
 (fn-traced
  [db [_ new-hash]]
  (-> db
      (assoc :location-hash new-hash))))

(rf/reg-event-fx
 ::init-song-delays
 (fn-traced
  [{:keys [db]} _]
  (let [delays (get-custom-delays-from-localstorage)]
    {:db (if-not (nil? delays)
           (-> db
               (update :custom-song-delay
                       (fn [v]
                         (merge {} v delays))))
           db)
     :dispatch [::song-delays-loaded]})))

(rf/reg-event-db
 ::song-delays-loaded
 (fn-traced
  [db _]
  (-> db
      (assoc :song-delays-loaded true))))

(rf/reg-event-fx
 ::init-song-bg-cache
 (fn-traced
  [{:keys [db]} _]
  (let [cache (get-from-localstorage "song-bg-cache")]
    {:db (if-not (nil? cache)
           (-> db
               (update :song-backgrounds
                       merge cache)
               (assoc :song-backgrounds-loaded? true))
           db)
     :dispatch [::song-bgs-loaded]})))

(rf/reg-event-db
 ::song-bgs-loaded
 (fn-traced
  [db _]
  (. js/console (log "song backgrounds loaded"))
  db))

(reg-set-attr ::set-initialized? :initialized?)
(reg-set-attr ::set-loop? :loop?)
(reg-set-attr ::set-audio-events :audio-events)
(reg-set-attr ::set-song-duration :song-duration)
(reg-set-attr ::set-current-frame :current-frame)
(reg-set-attr ::set-audio :audio)
(reg-set-attr ::set-lyrics :lyrics)
(reg-set-attr ::set-lyrics-delay :lyrics-delay)
(reg-set-attr ::set-lyrics-loaded? :lyrics-loaded?)
(reg-set-attr ::set-display-lyrics? :display-lyrics?)
(reg-set-attr ::set-can-play? :can-play?)
(reg-set-attr ::set-current-view :current-view)
(reg-set-attr ::set-player-current-time :player-current-time)
(reg-set-attr ::set-playing? :playing?)
(reg-set-attr ::set-pageloader-active? :pageloader-active?)

(rf/reg-event-db
 ::toggle-display-lyrics
 (fn-traced
  [db _]
  (-> db
      (update :display-lyrics? not))))

(rf/reg-event-fx
 ::set-current-song
 (fn-traced
  [{:keys [db]} [_ song-name]]
  {:db (-> db
           (assoc :current-song song-name))
   :dispatch-n [
                ;; [::fetch-bg song-name]
                [::set-lyrics-delay (get-in db [:custom-song-delay song-name] (get db :lyrics-delay))]]}))

(reg-set-attr ::set-player-status :player-status)
(reg-set-attr ::set-highlight-status :highlight-status)

(rf/reg-event-fx
 ::fetch-lyrics
 (fn-traced [{:keys [db]} [_ name process]]
            {:db (-> db
                     (assoc :lyrics-loaded? false)
                     (assoc :lyrics-fetching? true))
             :http-xhrio {:method :get
                          :uri (str (get db :base-storage-url "") "/lyrics/" name ".edn")
                          :timeout 8000
                          :response-format (ajax/text-response-format)
                          :on-success [::handle-set-lyrics-success]}}))

(rf/reg-event-db
 ::handle-set-lyrics-success
 (fn-traced
  [db [_ lyrics]]
  (let [l (-> lyrics
              (reader/read-string))]
              ;; (preprocess-frames))]
    (-> db
        (assoc :lyrics l)
        (assoc :lyrics-fetching? false)
        (assoc :lyrics-loaded? true)))))

(rf/reg-event-fx
 ::play
 (fn-traced
  [{:keys [db]} _]
  {:db (-> db
           (assoc :playing? true))}))

(defn highlight-if-same-id [id]
  (fn [evt]
    (if (= id (:id evt))
      (assoc evt :highlighted? true)
      evt)))

(rf/reg-event-db
 ::highlight-frame-part
 (fn-traced [db [_ frame-id part-id]]
            (if (and  (get db :current-frame)
                      (= frame-id (:id (get db :current-frame))))
              (-> db
                  (update-in [:current-frame :events]
                             (fn [evts]
                               (mapv (highlight-if-same-id part-id) evts))))
              db)))

;; (rf/reg-event-db
 ;; ::save-custom-song-delays-to-localstorage
 ;; (fn-traced [db _]
            ;; (save-custom-delays-to-localstore (:custom-song-delay db))
            ;; db))

(rf/reg-event-fx
 ::set-custom-song-delay
 (fn-traced
  [{:keys [db]} [_ song-name delay]]
  {:db (-> db
           (assoc-in [:custom-song-delay song-name] delay))
   :dispatch [::save-custom-song-delays-to-localstorage]}))

(rf/reg-event-fx
 ::modal-push
 (fn-traced
  [{:keys [db]} [_ modal]]
  {:db (-> db
           (update :modals conj modal))
   :dispatch [::modal-activate]}))

(rf/reg-event-db
 ::modal-activate
 (fn-traced
  [db _] db))

(rf/reg-event-db
 ::modal-pop
 (fn-traced
  [db _]
  (-> db
      (update :modals pop))))

(rf/reg-event-fx
 ::search-images
 (fn-traced
  [{:keys [db]} [_ q callback-event]]
  {:db db
   :http-xhrio {:method :get
                :timeout 8000
                :uri (str search/base-url
                          "?cx="  search/ctx-id
                          "&key=" search/api-key
                          "&q=" q)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success callback-event
                :on-failure [::print-arg]}}))

(rf/reg-event-fx
 ::print-arg
 (fn-traced
  [{:keys [db]} [_ & opts]]
  (cljs.pprint/pprint opts)
  {:db db}))

(rf/reg-event-fx
 ::fetch-bg
 (fn-traced
  [{:keys [db]} [_ title]]
  (let [cached (get-in db [:song-backgrounds title] nil)]
    (merge
     {:db db}
     (cond
       (not (nil? cached)) {:dispatch [::generate-bg-css cached]}
       fetch-bg-from-web-enabled?    {:dispatch [::search-images title [::handle-fetch-bg]]}
       :else {:dispatch [::handle-bg-complete]})))))

(rf/reg-event-fx
 ::handle-fetch-bg
 (fn-traced
  [{:keys [db]} [_ res]]
  (let [candidate-image (search/extract-candidate-image res)]
    {:db (if-not (nil? candidate-image)
           (-> db
               (assoc :bg-image (:url candidate-image)))
           db)
     :dispatch-n [[::generate-bg-css (:url candidate-image)]
                  [::cache-song-bg (:current-song db) (:url candidate-image)]]})))

(rf/reg-event-fx
 ::generate-bg-css
 (fn-traced
  [{:keys [db]} [_ url]]
  {:db (-> db
         (assoc :bg-style {:background-image (str "url(\"" url "\")")
                           :background-size "cover"}))
   :dispatch [::generate-bg-css-complete]}))

(rf/reg-event-fx
 ::generate-bg-css-complete
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::handle-bg-complete]}))

(rf/reg-event-db
 ::handle-bg-complete
 (fn-traced [db _] db))

                        ;; :transition "background-image 5s ease-out"}))))

(rf/reg-event-fx
 ::cache-song-bg
 (fn-traced
  [{:keys [db]} [_ song-name bg-url]]
  {:db (-> db
           (assoc-in [:song-backgrounds song-name] bg-url))
   :dispatch [::common-events/save-to-localstorage "song-bg-cache"
              (-> db
                  (assoc-in [:song-backgrounds song-name] bg-url)
                  :song-backgrounds)]}))


(rf/reg-event-fx
 ::initial-audio-setup
 (rf/after
  (fn [db _]
    (. js/console (log "Initial Audio Setup"))
    (let [audio (. js/document (getElementById "main-audio"))
          audio-events (aud/setup-audio-listeners audio)]
      (go-loop [e (<! audio-events)]
        (when-not (nil? e)
          (aud/process-audio-event e)
          (recur (<! audio-events))))
      (rf/dispatch [::set-audio audio])
      (rf/dispatch [::set-audio-events audio-events]))))
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::initial-audio-setup-complete]}))

(rf/reg-event-db
 ::initial-audio-setup-complete
 (fn-traced
  [db _]
  (. js/console (log "Initial Audio Setup Complete!"))
  (-> db (assoc :initial-audio-setup-complete? true))))
