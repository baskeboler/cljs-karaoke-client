(ns cljs-karaoke.views.seek-buttons)

(defn right-seek-component [seek-fn]
  [:a.right-seek-btn
   {:on-click seek-fn
    :title "Seek forward ten seconds"
    :aria-label "Seek forward ten seconds"}
   [:i.fas.fa-forward]
   [:span.seek-btn-label "+10s"]])

(defn left-seek-component [seek-fn]
  [:a.left-seek-btn
   {:on-click seek-fn
    :title "Seek backward ten seconds"
    :aria-label "Seek backward ten seconds"}
   [:i.fas.fa-backward]
   [:span.seek-btn-label "-10s"]])

(defn seek-component [fw-fn bw-fn]
  [:div
   [left-seek-component bw-fn]
   [right-seek-component fw-fn]])
