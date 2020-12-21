(ns cljs-karaoke.worker.impl)


(defmulti handler :cmd)

(defmethod handler :default
  [message]
  (println "Unexpected message: " message))
