(ns build
  (:require [shadow.cljs.devtools.api :as shadow]
            [clojure.java.shell :refer [sh]]
            [hiccup.page :refer [html5]]))

(defn sh! [command]
  (println command)
  (println (sh "bash" "-c" command)))

(defn watch []
  (shadow/watch :app))

(defn- meta-tag [name content]
  [:meta {:name name
          :content content}])

(defn seo-page [song]
  [:html
   [:head
    [:meta {:charset :utf-8}]
    (meta-tag
     "twitter:image:src"
     "https://repository-images.githubusercontent.com/166899229/7b618b00-a7ff-11e9-8b17-1dfbdd27fe74")
    (meta-tag "twitter:site" "@baskeboler")
    (meta-tag "twitter:card" "summary_large_image")
    (meta-tag :title  (str "Karaoke - " song))
    (meta-tag :description  "Karaoke Party")
    (meta-tag "twitter:title" (str "Karaoke Party :: " song))
    (meta-tag "twitter:description" (str "Online Karaoke Player. Sing " song " online!"))
    (meta-tag "og:image" "https://repository-images.githubusercontent.com/166899229/7b618b00-a7ff-11e9-8b17-1dfbdd27fe74")
    (meta-tag "og:site_name" "Karaoke Party")
    (meta-tag "og:type" "website")
    (meta-tag "og:url" (str "https://karaoke.uyuyuy.xyz/songs/" song))
    (meta-tag "og:description" "Karaoke Party. Online Karaoke player.")
    [:title (str "Karaoke Party :: "
                 song)]]
   [:body
    [:script
     (str "location.assign('/#/songs/" song "');")]]])

(defn prerender []
  (let  [songs  (clojure.edn/read-string (slurp "resources/public/data/songs.edn"))]
    (doall
     (doseq [s    songs]
       (println "Prerendering " s)
       (spit (str "public/songs/" s ".html")
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
  (when (= :release (:shadow.build/mode build-state))
   (prerender))
  build-state)

(defn create-docker-image []
  (shadow/release :app)
  (sh! "docker build -t cljs-karaoke-client ."))
