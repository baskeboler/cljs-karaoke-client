(ns cljs-karaoke.lyrics
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [com.rpl.specter :as s :include-macros true]
            [cljs.core :as core :refer [random-uuid]]))

(def frame-text-limit 128)
(def rand-uuid random-uuid)
(defn set-event-id [event]
  (if-not (nil? (:id event))
    (assoc event :id (random-uuid))
    event))

(defn to-relative-offset-events [base-offset]
  (fn [event]
    (s/transform [:offset] #(- % base-offset) event)))

(defn to-relative-offset-events-with-id [base-offset]
  (comp set-event-id (to-relative-offset-events base-offset)))

(defn update-events-to-relative-offset [base-offset]
  (fn [events]
    (s/transform [s/ALL] (to-relative-offset-events base-offset) events)))

(defn update-events-to-relative-offset-with-id [base-offset]
  (fn [events]
    (s/transform [s/ALL] (to-relative-offset-events-with-id base-offset) events)))

(defn to-relative-frame-offsets [frames]
  (reduce
   (fn [res fr]
     (let [last-frame (last res)
           new-offset (if-not (nil? last-frame)
                        (- (:offset fr) (:offset last-frame))
                        (:offset fr))
           new-frame (->> fr
                          (s/setval [:relative-offset] new-offset)
                          (s/setval [:id] (random-uuid)))
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
  (s/transform [s/ALL :events s/ALL]
               (fn [evt]
                 (assoc evt :id (rand-uuid)))
               frames))

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

(defprotocol LyricsDisplay
  (set-text [self t])
  (reset-progress [self])
  (inc-progress [self])
  (get-progress [self]))

(defrecord RLyricsDisplay [text progress progress-chan])

(extend-protocol LyricsDisplay
  RLyricsDisplay
  (set-text [self t] (-> self (assoc :text t)))
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
