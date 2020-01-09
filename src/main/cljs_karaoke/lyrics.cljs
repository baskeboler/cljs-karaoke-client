(ns cljs-karaoke.lyrics
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [cljs.core :as core :refer [random-uuid]]
            [cljs-karaoke.protocols :as protocols
             :refer [set-text reset-progress inc-progress
                     get-progress get-text get-offset played?
                     get-current-frame]]))

(def frame-text-limit 128)
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
      :else false)))
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
  (->map [{:keys [text offset id type  ] :as this}]
    {:text   text
     :offset offset
     :id     id
     :type   type})
  LyricsFrame
  (->map [{:keys [id events offset type ] :as this}]
    {:id     id
     :events (map ->map events)
     :offset offset
     :type   type}))
