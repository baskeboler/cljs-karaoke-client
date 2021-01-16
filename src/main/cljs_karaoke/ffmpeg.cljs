(ns cljs-karaoke.ffmpeg
  (:require [shadow.loader :as loader :refer [loaded? load with-module]]
            ;; ["@ffmpeg/ffmpeg" :as ffmpeg-lib :refer [createFFmpeg fetchFile]]
            [cljs.core.async :as async :refer [go go-loop <! >! chan promise-chan]]))
;; (defonce ffmpeg (createFFmpeg #js {:log true}))

;; (when-not (.isLoaded ffmpeg)
;;   (def ffpr
;;     (promise-chan))
;;   (..
;;    (.load ffmpeg)
;;    (then #(async/put! ffpr :ok)))

;;   (go-loop [_ (<! ffpr)]
;;     (println "testing ffmpeg")
;;     (.. (fetchFile "https://baskeboler.github.io/cljs-karaoke/mp3/ABBA-ABBA%20%20%20Fernando.mp3")
;;         (then (fn [data]
;;                 (.. ffmpeg
;;                     (FS "writeFile" "fernando.mp3" data))))
;;         (then (fn []
;;                 (.. ffmpeg
;;                     (run "-i" "fernando.mp3" "fernando.opus")))))))


(defn load-ffmpeg []
  (when-not (loaded? "ffmpeg")
    (load "ffmpeg")))


(defn fetch-file [name]
  (with-module "ffmpeg"
    ;; (require '[cljs-karaoke.modules.ffmpeg :as lib])
    (cljs-karaoke.modules.ffmpeg/fetch-file name)))
