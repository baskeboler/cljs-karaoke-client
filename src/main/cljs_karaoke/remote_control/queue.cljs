(ns cljs-karaoke.remote-control.queue
  (:require [cljs.core.async :as async :refer [go go-loop <! >! chan]]))
(defonce remote-commands-queue (chan))


(defn ^export queue-remote-command [cmd]
  (go
    (>! remote-commands-queue cmd)))

(println "Remote control queue initiated.")
