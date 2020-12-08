(ns cljs-karaoke.lyrics
  (:require [reagent.core :as r]
            [clojure.string :as str]
            ;; ["chart.js"]
            [chart-cljs.core :as chart]
            [thi.ng.color.core :as color]
            [cljs.core :as core :refer [random-uuid]]
            [cljs-karaoke.protocols :as protocols
             :refer [set-text reset-progress inc-progress
                     get-progress get-text get-offset played?
                     get-current-frame get-frame-count get-word-count
                     get-avg-words-per-frame get-max-words-frame
                     get-min-words-frame get-frames-chart-data
                     get-words-chart-data]]))

(def frame-text-limit 96)
(def rand-uuid random-uuid)
(defn set-event-id [event]
  (if-not (nil? (:id event))
    (assoc event :id (random-uuid))
    event))

(defn to-relative-offset-events [base-offset]
  (fn [event]
    (-> event
        (update :offset #(- % base-offset)))))
    ;; (s/transform [:offset] #(- % base-offset) event)))

(defn to-relative-offset-events-with-id [base-offset]
  (comp set-event-id (to-relative-offset-events base-offset)))

(defn update-events-to-relative-offset [base-offset]
  (fn [events]
    (mapv (to-relative-offset-events base-offset) events)))
    ;; (s/transform [s/ALL] (to-relative-offset-events base-offset) events)))

(defn update-events-to-relative-offset-with-id [base-offset]
  (fn [events]
    (mapv (to-relative-offset-events-with-id base-offset) events)))
    ;; (s/transform [s/ALL] (to-relative-offset-events-with-id base-offset) events)))

(defn to-relative-frame-offsets [frames]
  (reduce
   (fn [res fr]
     (let [last-frame (last res)
           new-offset (if-not (nil? last-frame)
                        (- (:offset fr) (:offset last-frame))
                        (:offset fr))
           new-frame (->> fr
                          (assoc :relative-offset new-offset)
                          (assoc :id (random-uuid)))
                          ;; (s/setval [:relative-offset] new-offset)
                          ;; (s/setval [:id] (random-uuid)))
           #_(-> fr
                 (assoc :relative-offset new-offset)
                 (set-event-id))]
       (conj (vec res) new-frame)))
   []
   (vec frames)))

(defn- event-text [evt]
  (if-not (or
           (nil? evt)
           (nil? (:text evt)))
    (str/trim (:text evt))
    ""))

(defn- partition-fn [evt]
  (not (or
        (str/starts-with? (event-text evt) "\\")
        (str/starts-with? (event-text evt) "/")
        (str/ends-with? (event-text evt) ".")
        (str/ends-with? (event-text evt) "?")
        (str/ends-with? (event-text evt) "!"))))

(defn- partition-events [events]
  (loop [res []
         events-1 events]
    (let [[new-grp rst] (split-with partition-fn events-1)
          new-grp (concat res (take-while (comp not partition-fn) rst))
          new-rst (drop-while (comp not partition-fn) rst)]
            ;; new-rst rst]
      (if  (and (empty? new-grp)
                (empty? new-rst))
        res
        (recur (conj res new-grp) new-rst)))))

(defn- partition-events-2 [events]
  (reduce
   (fn [res evt]
     (let [last-grp (last res)
           last-grp-length (reduce + 0 (map event-text last-grp))]
       (if (< last-grp-length frame-text-limit)
         (let [new-last (conj last-grp evt)
               new-res (conj (-> res
                                 reverse rest reverse vec)
                             new-last)]
           new-res)
         (concat res [[evt]]))))
   [[]] events))

(defn frame-text-string [frame]
  (let [events (:events frame)]
    (->> events
         (map :text)
         (apply str))))

(defn build-frame [grp]
  {:type :frame-event
   :id (random-uuid)
   :events (map set-event-id (vec grp))
   :ticks (reduce min js/Number.MAX_VALUE (map :ticks (vec grp)))
   :offset (reduce min js/Number.MAX_VALUE (map :offset (vec grp)))})

(defn split-frame [frame]
  (let [grps (partition-events-2 (:events frame))
        frames (mapv build-frame grps)]
    frames))

(defn needs-split? [frame]
  (> (count (frame-text-string frame)) frame-text-limit))

(defn split-frames-if-necessary [frames]
  (let [frame-grps (mapv (fn [fr]
                           (if (needs-split? fr)
                             (split-frame fr)
                             [fr]))
                         frames)]
    (apply concat frame-grps)))

;; (defn random-uuid [] (cljs.core/random-uuid))
(defn set-ids [frames]
  #_(s/transform [s/ALL]
                 (fn [fr]
                   (s/setval [:id] (rand-uuid) fr)))
  (mapv
   (fn [frame]
     (-> frame
         (update
          :events
          (fn [evts]
            (->> evts
                 (mapv
                  (fn [evt]
                    (-> evt
                        (assoc :id (rand-uuid))))))))))
   frames))
  ;; (s/transform [s/ALL :events s/ALL]
               ;; (fn [evt]
                 ;; (assoc evt :id (rand-uuid))
               ;; frames))

(defn preprocess-frames [frames]
  (let [no-dupes
        (map (fn [fr]
               (let [events (->> (into #{} (:events fr))
                                 vec
                                 (sort-by :offset))]
                 (-> fr
                     (assoc :events events))))
             frames)
        frames-2 (split-frames-if-necessary (vec no-dupes))
        with-offset
        (mapv (fn [fr]
                (-> fr
                    (assoc :offset
                           (reduce min 1000000
                                   (map :offset (:events fr))))))
              frames-2)
        with-relative-events        (mapv
                                     (fn [frame]
                                       (-> frame
                                           #(update % :events
                                                    (update-events-to-relative-offset-with-id
                                                     (:offset %)))))
                                     with-offset)]
    ;; frames-2
    (-> with-relative-events
        (to-relative-frame-offsets))))
(defrecord RLyricsDisplay [text progress progress-chan])

(extend-protocol protocols/LyricsDisplay
  RLyricsDisplay
  (set-text [self t]
    (-> self (assoc :text t)))
  (reset-progress [self] (-> self (assoc :progress 0)))
  (inc-progress [self]
    (-> self (update :progress inc)))
  (get-progress [self]
    (let [c (count (:text self))]
      (if (>= (:progress self) (count c))
        1.0
        (/ (float (:progress self))
           (float c))))))

;; (defn lyrics-display [t]
  ;; (->RLyricsDisplay t 0))

;; (def pepe-test
  ;; (lyrics-display "hola pepe"))

;; (-> pepe-test
;;     (set-text "lalala")
;;     #(iterate inc-progress %)
;;     (take 5)
;;     (last))
(defrecord ^:export LyricsEvent [id text ticks offset type])
  ;; IEquiv
  ;; (-equiv [_ other]
  ;;   (and (instance? LyricsEvent other)
  ;;        (= id (:id other))))

  ;; IPrintWithWriter
  ;; (-pr-writer [a writer _]
  ;;   (-write writer (str "#lyrics-event \""
  ;;                       (pr-str {:id     id
  ;;                                :text   text
  ;;                                :ticks  ticks
  ;;                                :offset offset
  ;;                                :type   type})
  ;;                       "\""))))
(defn read-lyrics-event [e]
  (let [values (cljs.reader/read-string e)]
    (map->LyricsEvent values)))
(cljs.reader/register-tag-parser! "cljs-karaoke.lyrics.LyricsEvent" read-lyrics-event)

(extend-protocol protocols/PLyrics
  LyricsEvent
  (get-text [this]
    (:text this))
  (get-offset [this]
    (:offset this))
  (played? [this delta]
    (<= (:offset this) delta)))

(defrecord ^:export LyricsFrame [id events type ticks offset])

(extend-protocol  protocols/PLyrics
  LyricsFrame
  (get-text [this]
    (frame-text-string this))
  (get-offset [this]
    (:offset this))
  ;; (->> (:events this)
         ;; (sort-by get-offset)
         ;; first
         ;; :offset
         ;; (+ (:offset this)))
  (played? [this delta]
    (let [last-event-offset (->> (:events this)
                                 last
                                 :offset
                                 (+ (:offset this)))]
      (<= last-event-offset delta)))
  (get-next-event [this offset]
    (->> (:events this)
         (filterv #(< offset (+ (:offset this) (:offset %))))
         first))
  object
  (get-text [this]
    (cond
      (= :frame-event (:type this)) (-> (map->LyricsFrame this)
                                        get-text)
      (= :lyrics-event (:type this)) (-> (map->LyricsEvent this)
                                         get-text)
      :else ""))
  (get-offset [this] (:offset this))
  (played? [this delta]
    (cond
      (= :frame-event (:type this)) (-> (map->LyricsFrame this)
                                        (played? delta))
      (= :lyrics-event (:type this)) (-> (map->LyricsEvent this)
                                         (played? delta))
      :else false))
  nil
  (get-text [this] "")
  (get-offset [this] 0)
  (played? [this delta] true))
(defn ^:export create-lyrics-event [{:keys [offset text]}]
  (map->LyricsEvent
   {:id     (str (random-uuid))
    :type   :lyrics-event
    :text   text
    :offset offset
    :ticks  -1}))
(defn ^:export create-frame [obj]
  (->
   (->> obj
        map->LyricsFrame)
   (update :events #(map  map->LyricsEvent %))))

(defrecord ^:export Song [name frames])

(extend-protocol   protocols/PSong
  Song
  (get-current-frame  [this time]
    (loop [the-frames (:frames this)
           result     nil]
      (cond
        (empty? the-frames)         result
        (not (played? result time)) result
        :else                       (recur (rest the-frames) (first the-frames))))))

(defn ^:export create-song [name frame-seq]
  (map->Song
   {:name   name
    :frames (map create-frame frame-seq)}))

(defprotocol ToMap
  (->map [this]))

(extend-protocol ToMap
  LyricsEvent
  (->map [{:keys [text offset id type] :as this}]
    {:text   text
     :offset offset
     :id     id
     :type   type})
  LyricsFrame
  (->map [{:keys [id events offset type] :as this}]
    {:id     id
     :events (map ->map events)
     :offset offset
     :type   type}))

(defn word-count [text]
  (if (str/blank? (str/trim text))
    0
    (let [words (str/split text #" ")]
      (count words))))

(declare timed-frames timed-words)
(extend-protocol protocols/PLyricsStats
  Song
  (get-frame-count [this]
    (-> this
        :frames
        count))
  (get-word-count [this]
    (let [frames (:frames this)
          words-per-frame (map (comp word-count protocols/get-text) frames)]
      (reduce + 0 words-per-frame)))
  (get-avg-words-per-frame [this]
    (/ (get-word-count this)
       (get-frame-count this)))
  (get-max-words-frame [this]
    (let [frames (:frames this)
          wpf (map (comp word-count protocols/get-text) frames)]
      (apply max wpf)))
  (get-min-words-frame [this]
    (let [frames (:frames this)
          wpf (map (comp word-count protocols/get-text) frames)]
      (apply min wpf)))
  (get-frames-chart-data [this interval-length]
    (timed-frames this interval-length))
  (get-words-chart-data [this interval-length]
    (timed-words this interval-length)))

(defn buckets
  "returns a list of intervals of step length over [start end]"
  [start end step]
  (let [points (concat (range start end step) [end])
        part1 (partition-all 2 points)
        part2 (partition-all 2 (rest points))]
    (filter #(= 2 (count %)) (interleave part1 part2))))

(defn aprox-song-duration [song]
  (let [frames (:frames song)
        last-frame (last frames)
        last-event (last (:events last-frame))]
    (int
     (+ (:offset last-frame) (:offset last-event) 2000))))

(defn- frames-in-interval [song interval]
  (let [[start end] interval
        frames (:frames song)]
    (count (filter
            #(and (> (:offset %) start)
                  (< (:offset %) end))
            frames))))

(defn- lyrics-evts [song]
  (let [frames  (:frames song)
        evts (map (comp :events
                        (fn [frame]
                          (update frame
                                  :events
                                  (fn [evts]
                                    (map #(update % :offset (partial + (:offset frame)))
                                         evts)))))
                  frames)]
    (apply concat evts)))

(defn- words-in-interval [song interval]
  (let [[start end] interval
        evts (lyrics-evts song)
        reducer (fn [res evt]
                  (let [t (:offset evt)]
                    (if (and (< t end) (> t start))
                      (str res (:text evt))
                      res)))
        filtered-evts (filter #(and (> (:offset %) start) (< (:offset %) end)) evts)
        words (reduce reducer ""
                      filtered-evts)]
    (word-count words)))

(defn timed-frames [song interval-length]
  (let  [intervals (buckets 0 (aprox-song-duration song) interval-length)]
    (map #(frames-in-interval song %) intervals)))

(defn timed-words [song interval-length]
  (let [intervals (buckets 0 (aprox-song-duration song) interval-length)]
    (map #(words-in-interval song %) intervals)))


;; (defn- show-chart-fn [canvas-id data labels label]
;;   (fn []
;;     (let [ctx        (.. js/document
;;                          (getElementById canvas-id)
;;                          (getContext "2d"))
;;           datasets (if (map? data)
;;                      (for [[k v] data]
;;                        {:data v
;;                         :label (apply str (rest (str k)))
;;                         :backgroundColor (:col (color/as-css (color/random-rgb)))})
;;                      [{:data data
;;                        :label label
;;                        :backgroundColor (:col (color/as-css (color/random-rgb)))}])  
;;           chart-data {:type                "line"
;;                       :responsive          true
;;                       :options {:title {:display true
;;                                         :text "stats"}}
;;                       :maintainAspectRatio false
;;                       :data                {:labels   labels
;;                                             :datasets datasets}}]
;;      (js/Chart. ctx (clj->js chart-data)))))

;; ;; (defn bar-chart-component [data labels label]
;;   (let [canvas-id  (str (gensym))
;;         show-chart (show-chart-fn canvas-id data labels label)]
;;     (r/create-class
;;      {:component-did-mount #(show-chart)
;;       :display-name        (str "bar-chart-component-" canvas-id)
;;       :reagent-render      (fn []
;;                              [:canvas {:id     canvas-id}])})))
;;                                        ;; :width  "100%"
                                       ;; :height 200}])})))
;; (defn ^:export frames-chart [song]
;;   (let [int-len 15000
;;         data    (get-frames-chart-data song int-len)
;;         data-words (get-words-chart-data song int-len)]
;;     [bar-chart-component
;;      {:frames data
;;       :words data-words}
;;      (map (comp str #(/ % 1000)) (take (count data) (iterate (partial + int-len) 0)))
;;      "frames"]))

(defn song-stats-chart-data [song]
  (let   [int-len     15000
          data-frames (get-frames-chart-data song int-len)
          data-words  (get-words-chart-data song int-len)
          datasets    (for [[k v] {:frames data-frames
                                   :words data-words}]
                        {:data            v
                         :label           (apply str (rest (str k)))
                         :backgroundColor (:col (color/as-css (color/random-rgb)))})
          labels      (map (comp str #(/ % 1000)) (take (count data-frames) (iterate (partial + int-len) 0)))]
    {:type                "line"
     :responsive          true
     :options             {:title {:display true
                                   :text    "stats"}}
     :maintainAspectRatio false
     :data                {:labels   labels
                           :datasets datasets}}))

(defn frames-chart [song]
  [chart/chart-component (song-stats-chart-data song)])
