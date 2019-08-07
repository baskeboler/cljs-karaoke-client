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
    (. vid (play))

    (go
      (<! (async/timeout 3000))
      (set! (. vid -class) "preview"))

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
                  [::set-reverb-analyser analyser2]]}))
                  
                  
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
   :audio-context       nil
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
