(ns mongo
  (:require [monger.core :as mg]
            [monger.collection :as mc])
  (:import [com.mongodb MongoOptions ServerAddress MongoClientSettings ConnectionString]
           [com.mongodb.client MongoClients MongoClient MongoDatabase MongoCollection]))

(def ^:dynamic ^String *monger-connection* nil)
(def ^:dynamic ^String *karaoke-db*)

(declare create-client get-database)


(def bg-collection "backgrounds")
(defn create-client [username password]
  (mg/connect-via-uri
   (format
    ;; "mongodb://%s:%s@cluster0-shard-00-00-gwbn9.mongodb.net:27017,cluster0-shard-00-01-gwbn9.mongodb.net:27017,cluster0-shard-00-02-gwbn9.mongodb.net:27017/test?ssl=true&replicaSet=Cluster0-shard-0&authSource=admin&retryWrites=true&w=majority"
    "mongodb+srv://%s:%s@cluster0-gwbn9.mongodb.net/test?retryWrites=true&w=majority"
    username
    password)))

(defn  get-database
  ([client ^String db-name]
   (-> client
       (mg/get-db  db-name)))
  ([^String db-name]
   (get-database  (:conn *monger-connection*) db-name)))

(defn ^MongoCollection collection [^MongoDatabase db ^String collection-name]
  (-> db
      (.getCollection collection-name)))

(defn get-background-image-map [db]
  (let [results (mc/find-maps db bg-collection)
        results (sort-by :updated_at results)
        results (map :bgMapping results)]
    (->> (for [[k v] (apply merge results)]
           [(name k) v])
         (into {}))))

(defn new-backgrounds []
  (get-background-image-map *karaoke-db*))

(when (and
       (System/getenv "MONGO_USER")
       (System/getenv "MONGO_PASSWORD"))
  (let [user     (System/getenv "MONGO_USER")
        password (System/getenv "MONGO_PASSWORD")]
    (alter-var-root #'*monger-connection* (constantly (:conn (create-client user password))))
    (alter-var-root #'*karaoke-db* (constantly (get-database *monger-connection* "karaoke")))
    (println "mongodb connection ready!")))

