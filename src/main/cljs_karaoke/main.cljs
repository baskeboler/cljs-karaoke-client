(ns cljs-karaoke.main
  (:require ["electron" :refer [app BrowserWindow crashReporter]]))

(def main-window (atom nil))

(defn init-browser []
  (reset! main-window ^js (BrowserWindow.
                            (clj->js {:width 800
                                      :height 600})))
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js @main-window "closed" #(reset! main-window nil)))

(defn main []
  (.start ^js crashReporter
          (clj->js
           {:companyName "ggsoft"
            :productName "karaoke"
            :submitURL "https://example.com/submit-endpoint"
            :autoSubmit false}))
  (.on ^js app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit ^js app)))
  (.on ^js app "ready" init-browser))

