(ns cljs-karaoke.views.cheatsheet
  (:require [clojure.string :as s]
            [cljs-karaoke.modals :as modals]))


(def shortcuts
  [{:label "stop"
    :shortcut [:esc]}
   {:label "load song"
    :shortcut [:l :s]}
   {:label "enable options in playbay mode"
    :shortcut [[:alt :o]]}
   {:label "enable control panel mode"
    :shortcut [[[:alt :h]]]}
   {:label "audio seek backwards"
    :shortcut [:left]}
   {:label "audio seek forward"
    :shortcut [:right]}
   {:label "loop mode"
    :shortcut [[:meta :shift :l]]}
   {:label "play"
    :shortcut [[:alt :shift :p]]}
   {:label "next song in playlist"
    :shortcut [[:shift :right]]}
   {:label "toasty!"
    :shortcut [:t :t]}
   {:label "show cheatsheet"
    :shortcut [:?]}])

(defn- kw->str [kw]
  (apply str (rest (str kw))))

(defn- ^String shortcut-item [item]
  (if (vector? item)
    (->> (map kw->str item)
         (s/join "-"))
    (kw->str item)))
    
(defn- shortcut-string [sc]
  (s/join " " (map shortcut-item sc)))


(defn shortcut-component [{:keys [label shortcut]}]
  [:tr
   [:td>p.heading label]
   [:td>p.heading (shortcut-string shortcut)]])

(defn cheatsheet-component []
  [:div.cheatsheet
   [:table.table
    (into
     [:tbody]
     (for [sc shortcuts]
       [shortcut-component sc]))]])

(defn ^:export show-cheatsheet []
  (modals/show-modal-card-dialog
   {:title "cheatsheet"
    :content [cheatsheet-component]}))
