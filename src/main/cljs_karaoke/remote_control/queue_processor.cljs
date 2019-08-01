(ns cljs-karaoke.remote-control.queue-processor
  (:require [cljs-karaoke.remote-control.queue :as queue :refer [remote-commands-queue]]
            [cljs.core.async :as async :refer [go go-loop <! >!]]
            [cljs-karaoke.remote-control.execute :refer [execute-command]]))

(go-loop [next-command (<! remote-commands-queue)]
  (execute-command next-command)
  (recur (<! remote-commands-queue)))

(println "Queue processor started")
