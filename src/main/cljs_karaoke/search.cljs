(ns cljs-karaoke.search
  (:require [ajax.core :as ajax]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [chan]]))

(def api-key "AIzaSyBIBQ2KPIDLzNqTMm76UcMJQ3qLTk6iYy0")
(def base-url "https://www.googleapis.com/customsearch/v1")
(def ctx-id "007074704954011898567:vq4nmfwmtgc")

(def search-resp (atom nil))

(defn is-bigger? [im1 im2]
  (let [s1 (* (:height im1) (:width im1))
        s2 (* (:height im2) (:width im2))]
    (> s1 s2)))

(def image-comparator (comparator is-bigger?))

(defn extract-candidate-image [res]
  (let [candidate-images (->> (map (comp first :imageobject :pagemap) (:items res))
                              (filter (comp not nil?)))]
    (->> candidate-images
         (sort image-comparator)
         first)))

(defn do-test-fetch-handle [res-chan]
  (fn [res]
    (async/put! res-chan (extract-candidate-image res))))

(defn query-url [q]
  (str base-url "?cx=" ctx-id "&key=" api-key "&q=" q))

(defn do-test-fetch
  ([q]
   (let [res-chan (chan)]
     (ajax/GET (query-url q)
               {:handler (do-test-fetch-handle res-chan)
                :response-format (ajax/json-response-format {:keywords? true})})
     res-chan))
  ([] (do-test-fetch "listen to your heart")))
