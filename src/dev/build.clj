(ns build
  (:require [shadow.cljs.devtools.api :as shadow]
            [clojure.java.shell :refer [sh]]
            [hiccup.page :refer [html5]]
            [clojure.tools.reader :as reader]
            [clojure.java.io :as io :refer [input-stream]]
            [clojure.string :as cstr]
            [mongo :as m])
            ;; [clojure.string :as str])
  (:import [java.net URLConnection URLEncoder URL]
           [java.nio.charset StandardCharsets]
           [java.time Instant]
           [java.nio.file Path Paths]))
(def site-url-prefix "https://karaoke-player.netlify.app")
(def background-images-file "resources/public/data/backgrounds.edn")
(def songs-file "resources/public/data/songs.edn")
(def delays-file "resources/public/data/delays.edn")
(def project-images-directory "resources/public/images")
(def project-images-path-prefix "/images")
(defn url->filename [url-str]
  (-> (URL. url-str)
      .toString
      (cstr/replace  #"\/|:|\.|-|#|\?" "_")))
      ;; last))
(def download-folder "./downloads/")

(defn- url-encode [s]
  (URLEncoder/encode s))

(def content-type->extension-mapping
  {"image/jpeg" ".jpg"
   "image/jpg"  ".jpg"
   "image/png"  ".png"
   "image/gif"  ".gif"
   "image/bmp"  ".bmp"})

;; It's checking if the file has an extension. If it doesn't, it's adding one.
(defn- normalize-filename [name input-str]
  (if-not (URLConnection/guessContentTypeFromName name)
    (str name (get content-type->extension-mapping
                   (URLConnection/guessContentTypeFromStream input-str)
                   ""))
    name))

;; It's downloading the images from the internet and saving them
;; to the local file system.
(defn download-file
  ([url filename]
   (with-open [in (input-stream url)]
     (let [filename (normalize-filename filename in)
           f (io/file filename)]
       (io/make-parents f)
       (io/copy in f)
       (println "Saved url to " filename)
       f)))
  ([url]
   (download-file url (str download-folder (url->filename url)))))

(declare get-images)
(defn download-all-images []
  (let [image-map (get-images)]
    (into {}
          (for [[filename url] image-map
                :let [file (try
                             (download-file url)
                             (catch Exception e
                               (println "failed to download " url)
                               nil))]]
            [filename file]))))
(defn- timestamp-extension []
  (-> (Instant/now) (.getEpochSecond)))

(defn- backup-background-images-file []
  (io/copy (io/file background-images-file)
           (io/file (str background-images-file ".bak." (timestamp-extension)))))

(defn- external-location? [l]
  (and l
       (cstr/starts-with? l "http")))

;; It's downloading the images from the internet and saving them
;; to the local file system.
(defn import-external-bg-images []
  (backup-background-images-file)
  (let [image-map (get-images)
        from-mongo (m/new-backgrounds)]
    (->> (for [[song-name image-url] (merge from-mongo  image-map)
               :when (external-location? image-url)
               :let [filename (url->filename image-url)
                     ;; is ()
                     download-filename (str project-images-directory "/covers/" filename)
                     f (try
                         (download-file image-url download-filename)
                         (catch Exception e
                           nil))
                     new-image-path (when-not (nil? f)
                                      (str project-images-path-prefix "/covers/" (.getName f)))]]
           (do
             (println song-name " -- " new-image-path)
             [song-name (if-not (nil? f)
                          new-image-path
                          image-url)]))
         (into {})
         (merge from-mongo image-map)
         (spit background-images-file))))

(defn sitemap-urls [songs]
  (map #(str site-url-prefix "/songs/" (url-encode %)) songs))

(defn get-songs []
  (reader/read-string (slurp songs-file)))

;; It's reading the delays file.
(defn get-delays []
  (reader/read-string (slurp delays-file)))

;; Checking if the url is valid.
(defn valid-url? [url]
  (print "checking url " url ": ")
  (if-not (external-location? url)
    true
    (try
      (with-open [_ (input-stream url)]
        (println "OK!")
        true)
      (catch Exception e
        (println "FAILED!")
        false))))

(defn get-images []
  (reader/read-string (slurp background-images-file)))

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

(def cmap
  {\' "\\'"})

(defn- escape-song-name [n]
  (cstr/escape n cmap))

(defn seo-page
  ([song offset image]
   [:html
    [:head
     [:meta {:charset :utf-8}]
     [:link {:async true
             :href "/css/main.css"
             :rel :stylesheet}]
     (meta-tag "viewport" "width=device-width, initial-scale=1")
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
     (meta-tag "og:url" (str "https://karaoke-player.netlify.app/songs/" (url-encode song)))
     (meta-tag "og:description" "Karaoke Party. Online Karaoke player.")
     [:link {:rel :canonical :href (str "https://karaoke-player.netlify.app/sing/" (url-encode song))}]
     [:title (str "Karaoke Party :: "
                  song)]
     [:style#_stylefy-constant-styles_]
     [:style#_stylefy-styles_]]
    [:body
     [:div#root]
     [:script {:src "/js/main.js"}]
     [:audio#main-audio {:crossOrigin :anonymous
                         :style "display:none;"}]
     [:audio#effects-audio {:crossOrigin :anonymous
                            :style "display:none;"
                            :src "/media/250-milliseconds-of-silence.mp3"}]
     [:video#main-video {:crossOrigin :anonymous
                         :style "display:none;"}]
     [:script
      (str "cljs_karaoke.app.load_song_global('"
           (escape-song-name song)
           "');")]]])
  ([song offset]
   (seo-page song offset default-seo-image))
  ([song]
   (seo-page song -1000)))

(defn- append-home-url [urls]
  (conj urls site-url-prefix))

;; Generating static html pages for each song.
(defn prerender []
  (let  [songs  (get-songs)
         delays (get-delays)
         images (get-images)]
    (doall
     (doseq [s    songs
             :let [delay (get delays s)
                   im (get images s default-seo-image)
                   im (if-not (external-location? im)
                        (str site-url-prefix im)
                        im)]]
       (println "Prerendering " s)
       (spit (str "public/songs/" s ".html")
             (html5 (rest (if-not (nil? delay)
                            (seo-page s delay im)
                            (seo-page s 0 im)))))))
    (println "used " (count (keys images)) " custom images for seo image tags")
    (println "Generating sitemap")
    (->> (sitemap-urls songs)
         append-home-url
         (cstr/join "\n")
         (spit "public/sitemap.txt"))
    (println "sitemap ready")))

(defn minify-css
  "Minifies the given CSS string, returning the result.
   If you're minifying static files, please use YUI."
  [css]
  (-> css
      (cstr/replace #"[\n|\r]" "")
      (cstr/replace #"/\*.*?\*/" "")
      (cstr/replace #"\s+" " ")
      (cstr/replace #"\s*:\s*" ":")
      (cstr/replace #"\s*,\s*" ",")
      (cstr/replace #"\s*\{\s*" "{")
      (cstr/replace #"\s*}\s*" "}")
      (cstr/replace #"\s*;\s*" ";")
      (cstr/replace #";}" "}")))

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
