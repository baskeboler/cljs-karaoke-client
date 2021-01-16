(ns cljs-karaoke.modules.ffmpeg
  (:require ["@ffmpeg/ffmpeg" :as ffmpeg-lib :refer [createFFmpeg fetchFile]]
            [cljs.core.async :as async :refer [go go-loop <! >! chan promise-chan]]))


(defonce ffmpeg (createFFmpeg #js {:log true}))

(defn load-ffmpeg []
  (let [c (promise-chan)]
    (go
      (if (.isLoaded ffmpeg)
        (>! c :already-loaded)
        (.. (.load ffmpeg)
            (then #(>! c :loaded)))))
    c))

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


(defn ^:export fetch-file [name]
  (let [c (chan 1)]
    (.. (fetchFile name)
        (then (fn [data]
                (async/put! c data))))
    c))

(defn ^:export save-to-memfs [path data]
  (let [c (promise-chan)]
    (.. ffmpeg
        (FS "writeFile" path data)
        (then #(async/put! c :ok)))
    c))


