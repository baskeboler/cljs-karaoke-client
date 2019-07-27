(ns cljs-karaoke.remote-control.queue
  (:require [cljs.core.async :as async :refer [go go-loop <! >! chan]]
            [cljs-karaoke.remote-control.execute :refer [execute-command]]))
(defonce remote-commands-queue (chan))

(go-loop [next-command (<! remote-commands-queue)]
  (execute-command next-command)
  (recur (<! remote-commands-queue)))

(defn ^export queue-remote-command [cmd]
  (go
    (>! remote-commands-queue cmd)))

(println "Remote control queue initiated.")
