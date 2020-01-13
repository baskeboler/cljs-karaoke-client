(ns build
  (:require [shadow.cljs.devtools.api :as shadow]
            [clojure.java.shell :refer [sh]]
            [hiccup.page :refer [html5]]
            [clojure.tools.reader :as reader]
            [clojure.string :as cstr])
  (:import [java.net URLEncoder]))

(def site-url-prefix "https://karaoke.uyuyuy.xyz")

(defn sitemap-urls [songs]
  (map #(str site-url-prefix "/songs/" %) songs))

(defn get-songs []
  (reader/read-string (slurp "resources/public/data/songs.edn")))

(defn get-delays []
  (reader/read-string (slurp "resources/public/data/delays.edn")))

(defn valid-url? [url]
  (try
    (with-open [_ (clojure.java.io/input-stream url)]
      true)
    (catch Exception e
      false)))

(defn get-images []
  (reader/read-string (slurp "resources/public/data/backgrounds.edn")))

(defn get-valid-images []
  (into
   {}
   (for [[k v] (get-images)
         :when (valid-url? v)]
     [k v])))

(defn sh! [command]
  (println command)
  (println (sh "bash" "-c" command)))

(defn watch []
  (shadow/watch :app))

(defn- meta-tag [name content]
  [:meta {:name name
          :content content}])

(def default-seo-image
  "https://repository-images.githubusercontent.com/166899229/7b618b00-a7ff-11e9-8b17-1dfbdd27fe74")

(defn seo-page
  ([song offset image]
   [:html
    [:head
     [:meta {:charset :utf-8}]
     (meta-tag
      "twitter:image:src"
      image)
     (meta-tag "twitter:site" "@baskeboler")
     (meta-tag "twitter:card" "summary_large_image")
     (meta-tag :title  (str "Karaoke - " song))
     (meta-tag :description  "Karaoke Party")
     (meta-tag "twitter:title" (str "Karaoke Party :: " song))
     (meta-tag "twitter:description" (str "Online Karaoke Player. Sing " song " online!"))
     (meta-tag "og:image"
               image)
     (meta-tag "og:site_name" "Karaoke Party")
     (meta-tag "og:type" "website")
     (meta-tag "og:url" (str "https://karaoke.uyuyuy.xyz/songs/" song))
     (meta-tag "og:description" "Karaoke Party. Online Karaoke player.")
     [:title (str "Karaoke Party :: "
                  song)]]
    [:body
     [:script
      (str "location.assign('/#/songs/" song "?offset=" offset "');")]]])
  ([song offset]
   (seo-page song offset default-seo-image))
  ([song]
   (seo-page song -1000)))

(defn prerender []
  (let  [songs  (get-songs)
         delays (get-delays)
         images (get-valid-images)]
    (doall
     (doseq [s    songs
             :let [delay (get delays s)
                   im (get images s default-seo-image)]]
       (println "Prerendering " s)
       (spit (str "public/songs/" s ".html")
             (html5 (rest (if-not (nil? delay)
                            (seo-page s delay im)
                            (seo-page s 0 im)))))))
    (println "Generating sitemap")
    (->> (sitemap-urls songs)
         (cstr/join "\n")
         (spit "public/sitemap.txt"))
    (println "sitemap ready")))

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
  ["css/main.css"])
   

(defn get-files [files]
  (->> files
       (map #(str target-dir "/" %))
       (map slurp)))

(defn ^:export setup-target-dir
  {:shadow.build/stage :compile-prepare}
  [build-state & args]
  (sh! "rm -rf public")
  (sh! "cp -rf resources/public public")
  (sh! "cp -rf ./node_modules/@fortawesome/fontawesome-free/webfonts ./public/")
  (sh! "npm run css-build")
  (->> css-files
       (mapv #(str target-dir "/" %))
       (mapv minify-css-inplace))
  build-state)

(defn ^:export build-css []
  (sh! "npm run css-build"))


(defn ^:export watch-css []
  (future
    (sh! "npm run css-watch")))


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
