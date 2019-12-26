(ns build
  (:require [shadow.cljs.devtools.api :as shadow]
            [clojure.java.shell :refer [sh]]
            [hiccup.page :refer [html5]])
  (:use [etaoin.api :as eta]))

;; (def driver (chrome-headless))
(defn sh! [command]
  (println command)
  (println (sh "bash" "-c" command)))

(defn watch []
  (shadow/watch :app))

(defn seo-page [song]
 [:html
   [:head
    [:meta {:name :title :content (str "Karaoke - " song)}]
    [:meta {:name :description :content "Karaoke Party"}]
    [:title song]]
   [:body
    [:script
     (str "location.assign('/#/songs/" song "');")]]])

(defn prerender []
  (let  [songs  (clojure.edn/read-string (slurp "resources/public/data/songs.edn"))]
    (doall
     (doseq [s    songs]
       (println "Prerendering " s)
       (spit (str "public/songs/" s)
             (html5 (rest (seo-page s))))))))

(defn minify-css
  "Minifies the given CSS string, returning the result.
   If you're minifying static files, please use YUI."
  [css]
  (-> css
      (clojure.string/replace #"[\n|\r]" "")
      (clojure.string/replace #"/\*.*?\*/" "")
      (clojure.string/replace #"\s+" " ")
      (clojure.string/replace #"\s*:\s*" ":")
      (clojure.string/replace #"\s*,\s*" ",")
      (clojure.string/replace #"\s*\{\s*" "{")
      (clojure.string/replace #"\s*}\s*" "}")
      (clojure.string/replace #"\s*;\s*" ";")
      (clojure.string/replace #";}" "}")))

(defn minify-css-inplace [path]
  (println "minifying " path)
  (->> (slurp path)
       minify-css
       (spit path)))

(def target-dir "public")

(def css-files
  ["css/main.css"
   "css/animista.css"])

(defn get-files [files]
  (->> files
       (map #(str target-dir "/" %))
       (map slurp)))

(defn ^:export setup-target-dir
  {:shadow.build/stage :compile-prepare}
  [build-state & args]
  (sh! "rm -rf public")
  (sh! "cp -rf resources/public public")
  (->> css-files
       (mapv #(str target-dir "/" %))
       (mapv minify-css-inplace))
  build-state)

(defn ^:export generate-seo-pages
  {:shadow.build/stage :flush
   :shadow.build/mode  :release}
  [build-state & args]
  (prerender)
  build-state)

(defn create-docker-image []
  (shadow/release :app)
  (sh! "docker build -t cljs-karaoke-client ."))
