(ns cljs-karaoke.songs-test
  (:require [cljs.test :as t :refer-macros [deftest testing is run-tests]]
            [cljs-karaoke.text :as text]
            [cljs-karaoke.protocols :as p]
            [cljs-karaoke.song-data :refer [data]]
            [cljs-karaoke.lyrics :as l]))
(deftest a-test
  (testing "a sample test"
       (is (= 1 1))))

(deftest word-spans
 (testing "generating word spans from a string"
  (let [input "hello world!"
        result [:span.words [:span.word "hello"] [:span.word "world!"]]]
    (is (= (text/split-into-word-spans input) result)))))

(defn- load-song-data [data]
  (->> data
       (map l/create-frame)
       (l/create-song "testing song")))


(deftest test-song-load
  (testing "song data loading"
    (let [s (load-song-data data)]
      (is (and
           (= "testing song" (:name s))
           (not= nil (:frames s))
           (= 9  (count (:frames s))))))))
