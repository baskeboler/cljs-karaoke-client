(ns cljs-karaoke.editor.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-karaoke.editor.events :as events]
            [cljs-karaoke.editor.subs]
            [cljs-karaoke.editor.view :as view]
            [mount.core :as mount :refer-macros [defstate]]
            [cljs-karaoke.protocols :as p]
            [cljs-karaoke.key-bindings :as kb :refer [disable-keybindings!]]))
(defmethod p/handle-route :editor [arg]
  (println "disabling keybindings in editor")
  ;; (disable-keybindings!)
  (mount/stop #'kb/keybindings)
  (:action arg))

(defn- start-editor []
  (println "starting up editor")
  (rf/dispatch-sync [:cljs-karaoke.editor.events/init]))

(defstate editor :start (start-editor))
  ;; :stop (println "stop editor"))

