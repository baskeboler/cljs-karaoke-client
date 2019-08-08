(ns cljs-karaoke.events.audio
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing :refer-macros [fn-traced]]
            [cljs-karaoke.events.common :as common :refer [reg-set-attr]]
            [ajax.core :as ajax]
            [cljs.core.async :as async :refer [<! >! go go-loop timeout chan]]
            [day8.re-frame.async-flow-fx]
            [cljs-karaoke.notifications :refer [notification add-notification]]
            [bardo.interpolate :as interpolate]))
(defn init-audio-input-flow []
  {:rules [{:when :seen?
            :events ::set-audio-context
            :dispatch [::fetch-reverb-buffer]}
           {:when :seen?
            :events ::set-reverb-buffer
            :dispatch [::setup-audio-input]}
           {:when :seen-all-of?
            :events [::set-audio-input
                     ::set-dry-gain
                     ::set-wet-gain
                     ::set-output-mix
                     ::set-reverb-analyser]
            :dispatch [::start-audio-input-spectrograph]}
           {:when :seen?
            :events ::refresh-audio-input-spectrograph
            :halt? true}]})

(defn convert-to-mono [input audio-context]
  (let [splitter (. audio-context (createChannelSplitter 2))
        merger (. audio-context (createChannelMerger 2))]
    (. input (connect splitter))
    (. splitter (connect merger 0 0))
    (. splitter (connect merger 0 1))
    merger))

(defn cross-fade [v dry-gain wet-gain]
  (let [gain1 (js/Math.cos (* v 0.5 js/Math.PI))
        gain2 (js/Math.cos (* (- 1.0 v) 0.5 js/Math.PI))]
    (set! (.-value (.-gain dry-gain)) gain1)
    (set! (.-value (.-gain wet-gain)) gain2)))

(defn create-lp-input-filter [audio-context]
  (let [f (.createBiquadFilter audio-context)]
    (set! (.-value (.-frequency f)) 2048)
    f))

(defn create-reverb! [audio-context reverb-buffer wet-gain]
  (let [convolver (.createConvolver audio-context)]
    (set! (.-buffer convolver) reverb-buffer)
    (. convolver (connect wet-gain))
    convolver))

(defn create-delay! [audio-context dtime dregen wet-gain]
  (let [delay-node (. audio-context (createDelay))
        gain-node (. audio-context (createGain))]
    (set! (.. delay-node -delayTime -value) dtime)
    (set! (.. gain-node -gain -value) dregen)
    (. gain-node (connect delay-node))
    (. delay-node (connect gain-node))
    (. delay-node (connect wet-gain))

    delay-node))

(defn on-stream [{:keys [db]} [_ stream]]
  (let [audio-context (get-in db [:audio-data :audio-context])
        vid (. js/document (querySelector "#main-video"))
        feedback-reduction? (get-in db [:audio-data :feedback-reduction?])
        reverb-buffer (get-in db [:audio-data :reverb-buffer])
        input (.createMediaStreamSource audio-context stream)
        audio-input (if feedback-reduction?
                      (do
                        (let [i (convert-to-mono input audio-context)
                              lp-filter (create-lp-input-filter audio-context)]
                          (. i (connect lp-filter))
                          lp-filter))
                      (convert-to-mono input audio-context))
        audio-filter (.createBiquadFilter audio-context)
        analyser1 (.createAnalyser audio-context)
        analyser2 (.createAnalyser audio-context)
        output-mix1 (.createGain audio-context)
        dry-gain1 (.createGain audio-context)
        wet-gain1 (.createGain audio-context)
        effect-input1 (.createGain audio-context)]
    (. audio-input (connect dry-gain1))
    (. audio-input (connect wet-gain1))
    (. audio-input (connect effect-input1))
    (. audio-input (connect analyser1))
    (. dry-gain1 (connect output-mix1))
    (. wet-gain1 (connect output-mix1))

    (. output-mix1 (connect analyser2))
    (. output-mix1 (connect (.-destination audio-context)))
    (set! (-> vid .-srcObject) stream)
    (set! (-> vid .-style .-display) "block")
    (.play vid)

    (go
      (<! (async/timeout 3000))
      (set! (.-className vid) "preview"))

    (cross-fade 1.0 dry-gain1 wet-gain1)
    (create-reverb! audio-context reverb-buffer wet-gain1)

    (create-delay! audio-context 0.5 0.5 wet-gain1)

    ;; (set! (.-srcObject vid) stream)
    ;; (set! (.-value (.-frequency audio-filter)) 60.0)
    ;; (set! (.-type audio-filter) "notch")
    ;; (set! (.-Q audio-filter) 10.0)
    ;; (.connect input audio-filter)
    ;; (.connect audio-filter analyser)
    ;; (reset! analyser analyser)
    ;; audio-filter))
    (println "Audio input init complete.")
    (add-notification (notification :success "Audio input initialized!"))
    {:db db
     :dispatch-n [[::set-audio-input audio-input]
                  [::set-dry-gain dry-gain1]
                  [::set-wet-gain wet-gain1]
                  [::set-effect-input effect-input1]
                  [::set-output-mix output-mix1]
                  [::set-clean-analyser analyser1]
                  [::set-reverb-analyser analyser2]
                  [::set-stream stream]
                  [::set-recording-enabled? true]]}))

(rf/reg-event-fx
 ::on-stream
 on-stream)

(def initial-audio-state
  {:feedback-reduction? true
   :reverb-buffer       nil
   :dry-gain            nil
   :wet-gain            nil
   :effect-input        nil
   :output-mix          nil
   :audio-input         nil
   :lp-input-filter     nil
   :clean-analyser      nil
   :reverb-analyser     nil
   :freq-data           nil
   :stream              nil
   :audio-context       nil
   :recording-enabled?  false
   :recorded-blobs      []
   :media-recorder      nil
   :recording-options   nil
   :recording?          false
   :constraints         {:audio {:optional [{:echoCancellation false}]}
                         :video true}})

(rf/reg-event-db
 ::init-audio-data
 (fn-traced
  [db _]
  (-> db
      (assoc :audio-data initial-audio-state))))

(rf/reg-event-fx
 ::init-pending-fns
 (rf/after)
 (fn-traced
  [{:keys [db]}]))

(reg-set-attr ::set-recording-enabled? [:audio-data :recording-enabled?])
(reg-set-attr ::set-feedback-reduction? [:audio-data :feedback-reduction?])
(reg-set-attr ::set-reverb-buffer [:audio-data :reverb-buffer])
(reg-set-attr ::set-dry-gain        [:audio-data :dry-gain])
(reg-set-attr ::set-wet-gain        [:audio-data :wet-gain])
(reg-set-attr ::set-effect-input    [:audio-data :effect-input])
(reg-set-attr ::set-output-mix      [:audio-data :output-mix])
(reg-set-attr ::set-audio-input     [:audio-data :audio-input])
(reg-set-attr ::set-lp-input-filter [:audio-data :lp-input-filter])
(reg-set-attr ::set-clean-analyser  [:audio-data :clean-analyser])
(reg-set-attr ::set-reverb-analyser [:audio-data :reverb-analyser])
(reg-set-attr ::set-freq-data       [:audio-data :freq-data])
(reg-set-attr ::set-audio-context   [:audio-data :audio-context])
(reg-set-attr ::set-stream [:audio-data :stream])
(reg-set-attr ::set-media-recorder [:audio-data :media-recorder])
(rf/reg-event-fx
 ::init-audio-context
 (fn-traced
  [{:keys [db]} _]
  (let [ctx (js/AudioContext.)]
    {:db db
     :dispatch [::set-audio-context ctx]})))

(rf/reg-event-fx
 ::fetch-reverb-buffer
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :http-xhrio {:method :get
                :uri  "/media/cardiod-rear-levelled.wav"
                :timeout 5000
                :response-format (-> (ajax/raw-response-format)
                                     (assoc :type :arraybuffer))
                :on-success [::handle-fetch-reverb-buffer-success]
                :on-failure [::handle-fetch-reverb-buffer-failure]}}))
(rf/reg-event-fx
 ::handle-fetch-reverb-buffer-success
 (fn-traced
  [{:keys [db]} [_ res]]
  (let [ctx (get-in db [:audio-data :audio-context])
        buf-promise (. ctx (decodeAudioData res))]
    (. buf-promise (then #(rf/dispatch [::set-reverb-buffer %])))
    {:db db})))

(rf/reg-event-db
 ::handle-fetch-reverb-buffer-failure
 (fn-traced
  [db [_ err]]
  (println "Failed to fetch reverb buffer" err)
  db))

(rf/reg-event-db
 ::append-recorded-blob
 (fn-traced
  [db [_ blob]]
  (-> db
      (update-in [:audio-data :recorded-blobs] conj blob))))


(defn get-user-media [args on-success on-failure]
  (cond
    (.-getUserMedia js/navigator) (-> js/navigator (.getUserMedia args on-success on-failure))
    (.-mozGetUserMedia js/navigator) (-> js/navigator (.mozGetUserMedia args on-success on-failure))
    :else (do
            (println "Could not find GetUserMedia function")
            nil)))

(defn get-microphone-input [{:keys [db]} _]
  (let [args (clj->js
              (get-in db [:audio-data :constraints]))]
    (get-user-media args
                    #(rf/dispatch [::on-stream %])
                    #(println "Failed to setup audio input" %))
    {:db db}))

(rf/reg-event-fx
 ::setup-audio-input
 get-microphone-input)

(def meter-count (int (/ 800.0 12)))

(defn get-raw-freq-data [analyser]
  (let [arr (js/Uint8Array. (.-frequencyBinCount analyser))]
    (. analyser (getByteFrequencyData arr))
    (-> arr aclone js->clj)))

(defn- avg [col]
  (let [sum (apply + col)]
    (/ sum (count col))))

(defn get-freq-data [analyser]
  (let [data (get-raw-freq-data analyser)]
    (->> (partition (int (/ (count data) meter-count)) data)
         (mapv (comp
                identity
                ;; dec
                #(/ % 205)
                avg)))))

(rf/reg-event-fx
 ::refresh-audio-input-spectrograph
 (fn-traced
  [{:keys [db]} _]
  (let [a (get-in db [:audio-data :reverb-analyser])
        data (get-freq-data a)]
    {:db db
     :dispatch [::set-freq-data data]})))

(rf/reg-event-fx
 ::start-audio-input-spectrograph
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :interval {:action :start
              :id :start-input-spectrograph
              :frequency 70
              :event [::refresh-audio-input-spectrograph]}}))

(rf/reg-event-fx
 ::init-audio-input
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::init-audio-context]
   :async-flow (init-audio-input-flow)}))

(def recorded-blobs (atom []))

(defn- get-recording-options []
  (let [mime-types ["video/webm;codecs=vp9"
                    "video/webm;codecs=vp8"
                    "video/webm"]
        result     (first (filter
                           (fn [mime-type]
                             (if-not (. js/MediaRecorder (isTypeSupported mime-type))
                               (do
                                 (. js/console (error (str mime-type " is not supported")))
                                 false)
                               true))
                           mime-types))]
    (if-not (nil? result)
      (clj->js {:mediaType result})
      (clj->js {:mediaType ""}))))

(defn get-media-recorder [stream options]
  (when-let [rec (try
                  (js/MediaRecorder. stream options)
                  (catch js/Error e
                    (. js/console (error "Failed to create MediaRecorder " e))
                    nil))]
    (. js/console (log "Created media recorder " rec " with options " options))
    rec))
(defn handle-data-available [event]
  (when (and (. event -data)
             (> (.. event -data -size) 0))
    (rf/dispatch [::append-recorded-blob (. event -data)])))

(defn start-recording [{:keys [db]} _]
  (let [stream (get-in db [:audio-data :stream])
        recording-blobs (get-in [:audio-data :recorded-blobs] db)
        options (get-recording-options)
        media-recorder (get-media-recorder stream options)]
    (set! (. media-recorder -onstop)
          (fn [event]
            (. js/console (log "Recording stopped: " event))))
    (set! (. media-recorder -ondataavailable) handle-data-available)
    (. media-recorder (start 10))
    (. js/console (log "Media Recorder started." media-recorder))
    {:db (-> db
             (assoc-in [:audio-data :recording?] true))
     :dispatch-n [[::set-media-recorder media-recorder]
                  [::set-recording-options options]]}))

(rf/reg-event-fx
 ::start-recording
 (fn-traced [cofx evt] (start-recording cofx evt)))

(rf/reg-event-fx
 ::test-recording
 (fn-traced
  [{:keys [db]} _]
  {:db db
   :dispatch [::start-recording]
   :dispatch-later [{:dispatch [::stop-recording]
                     :ms 10000}]}))

(defn- download-video [recorded-blobs]
  (let [video-blob (js/Blob. (clj->js recorded-blobs) #js {:type "video/webm"})
        url (.. js/window -URL (createObjectURL video-blob))
        a (.. js/document (createElement "a"))]
    (set! (.. a -style -display) "none")
    (set! (. a -href) url)
    (set! (. a -download) "karaoke.webm")
    (.. js/document -body (appendChild a))
    (. a (click))
    (js/setTimeout
     (fn []
       (.. js/document -body (removeChild a))
       (.. js/window -URL (revokeObjectURL url)))
     100)))

(defn stop-recording [{:keys [db]} _]
  (let [media-recorder (get-in db [:audio-data :media-recorder])
        recorded-blobs (get-in db [:audio-data :recorded-blobs])]
    (. media-recorder (stop))
    (. js/console (log "Stopped recording: " recorded-blobs))
    (download-video recorded-blobs)
    {:db (-> db
             (assoc-in [:audio-data :recording?] false))}))

(rf/reg-event-fx
 ::stop-recording
 (fn-traced [cofx evt] (stop-recording cofx evt)))
