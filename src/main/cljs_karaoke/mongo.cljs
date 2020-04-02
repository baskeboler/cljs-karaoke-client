(ns cljs-karaoke.mongo
  (:require ["mongodb-stitch-browser-sdk" :as mongo :refer [Stitch RemoteMongoClient AnonymousCredential]]))

(defonce app-id "karaoke-optdh")
(defonce client (. Stitch (initializeDefaultAppClient app-id)))

(def db (.. client (getServiceClient (.-factory RemoteMongoClient) "mongodb-atlas") (db "karaoke")))

(defn ^:export save-song [song-name n]
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
                       :owner_id (.. client -auth -user -id)}})
                  
              (clj->js
               {:upsert true})))))
      (then
       (fn []
         (.. db
             (collection "songs")
             (find (clj->js {:song-name song-name})
                   (clj->js {:limit 100}))
             asArray)))
      ;; (then
       ;; (fn [docs] (. js/console (log "got docs " docs))
         ;; (println "got docs: " (js->clj docs))))
      (catch
       (fn [err]
         (println "error " err)))))


(defn ^:export fetch-all []
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
            (println "there was an error fetching all songs." err)))))
