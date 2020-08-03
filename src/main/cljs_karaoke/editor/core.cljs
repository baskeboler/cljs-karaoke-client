(ns cljs-karaoke.editor.core
  (:require [cljs-karaoke.editor.events :as events]
            [cljs-karaoke.editor.subs :as subs]
            [cljs-karaoke.editor.view :as view]
            [mount.core :as mount]
            [cljs-karaoke.protocols :as p]))
            ;; [cljs-karaoke.key-bindings :refer [disable-keybindings!]]))
(defmethod p/handle-route :editor [arg]
  (println "disabling keybindings in editor")
  ;; (disable-keybindings!)
  (mount/stop #'cljs-karaoke.key-bindings/keybindings)
  (:action arg))
