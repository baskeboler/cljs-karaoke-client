(ns cljs-karaoke.components.stats)

(defn stats-item [label value]
  [:div.level-item.has-text-centered>div
   [:p.heading label]
   [:p.title value]])

(defn stats-row [& stats-items]
  [:div.level
   (for [i stats-items]
     i)])
