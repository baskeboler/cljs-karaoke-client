(ns cljs-karaoke.mongo
  (:require ["mongodb-stitch-browser-sdk" :as mongo :refer [Stitch RemoteMongoClient AnonymousCredential]]
            [cljs.core.async :as async :refer [<! >! go chan go-loop]]))
(defonce app-id "karaoke-optdh")
(defonce client (. Stitch (initializeDefaultAppClient app-id)))

(def db (.. client (getServiceClient (.-factory RemoteMongoClient) "mongodb-atlas") (db "karaoke")))

(defn wrap-chan [^js/Promise res]
  (let [c (chan 1)]
    (.. res
        (then (fn [docs]
                (go
                  (>! c docs)
                  (async/close! c))))
        (catch (fn [err]
                 (println "there was an error " err)
                 (async/close! c))))
    c))

(defn ^:export save-song [song-name n]
  (wrap-chan
   (.. client
       -auth
       (loginWithCredential (AnonymousCredential.))
       (then
        (fn [user]
          (.. db
              (collection "songs")
              (updateOne
               (clj->js
                {:song-name song-name})
               (clj->js
                {:$set {:lyrics (clj->js n)
                        :owner_id (.. client -auth -user -id)
                        :updated_at (js/Date.now)}})

               (clj->js
                {:upsert true})))))
       (then
        (fn []
          (.. db
              (collection "songs")
              (find (clj->js {:song-name song-name})
                    (clj->js {:limit 100}))
              asArray)))
       (catch
        (fn [err]
          (println "error " err))))))

(defn ^:export save-delays [delays]
  (wrap-chan
   (.. client
       -auth
       (loginWithCredential (AnonymousCredential.))
       (then
        (fn [_]
          (.. db
              (collection "custom-song-delays")
              (updateOne
               (clj->js {:owner_id (.. client -auth -user -id)})
               (clj->js {:$set {:owner_id     (.. client -auth -user -id)
                                :updated_at   (js/Date.now)
                                :delayMapping (clj->js delays)}})
               #js {:upsert true})))))))

(defn ^:export save-backgrounds [backgrounds]
  (wrap-chan
   (.. client
       -auth
       (loginWithCredential (AnonymousCredential.))
       (then
        (fn [user]
          (.. db
              (collection "backgrounds")
              (updateOne (clj->js  {:owner_id (.. client -auth -user -id)})
                         (clj->js {:$set {:owner_id   (.. client -auth -user -id)
                                          :updated_at (js/Date.now)
                                          :bgMapping  (clj->js backgrounds)
                                          :imported?  false}})
                         #js {:upsert true})))))))
(defn ^:export fetch-all-by-owner []
  (wrap-chan
   (.. client
       -auth
       (loginWithCredential (AnonymousCredential.))
       (then
        (fn [user]
          (.. db
              (collection "songs")
              (find (clj->js {:owner_id (.. client -auth -user -id)})
                    (clj->js {:limit 100}))
              asArray)))
       (then
        #(clj->js % :keywordize-keys true))
       (catch
        (fn [err]
          (println "there was an error fetching all songs." err))))))

(defn ^:export all-backgrounds []
  (wrap-chan
   (.. client
       -auth
       (loginWithCredential (AnonymousCredential.))
       (then
        (fn [_]
          (.. db
              (collection "backgrounds")
              ;; -backgrounds
              (find #js {})
              ;; (sort #js {"updated_at" 1})
              asArray)))
       (then
        #(js->clj % :keywordize-keys true)))))

(defn ^:export  merged-bgs []
  (wrap-chan
   (.. client
       -auth
       (loginWithCredential (AnonymousCredential.))
       (then
        (fn [_]
          (.. db
              (collection "backgrounds")
              (find #js {})
              asArray)))
       (then
        (fn [res]
          (println "processing " res)
          (->> (-> res
                   (js->clj :keywordize-keys false))
               (map #(get % "bgMapping"))
               (map #(js->clj % :keywordize-keys false))
               (concat [{}])
               (apply merge))))))) 

(defn ^:export fetch-all []
  (wrap-chan
   (.. client
       -auth
       (loginWithCredential (AnonymousCredential.))
       (then
        (fn [user]
          (.. db
              (collection "songs")
              (find #js {}
                    #js {:limit 100})
              asArray)))
       (then
        #(clj->js % :keywordize-keys true))
       (catch
        (fn [err]
          (println "there was an error fetching all songs." err)
          (throw err))))))

