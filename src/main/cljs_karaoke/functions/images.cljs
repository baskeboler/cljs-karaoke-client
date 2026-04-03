(ns cljs-karaoke.functions.images
  (:require [goog.string :as gstr]
            [cljs.core.async :as async]))

(def api-key "KbhjpGKMMpRFbIQQf7bqi4aK62YcsKWybwsJNgOK4qEzTE9jjArdZyNu")

(def api-endpoint "https://api.pexels.com/v1/search")

(defn search-bg-images
  [query]
  (let [url (str api-endpoint "?query=" (gstr/urlEncode query) "&size=small")
        ret (async/promise-chan)]
    ;; (js/console.log "url=" url)

    (-> (js/fetch url
                  (clj->js {"method" "GET"
                            "headers"
                            {"Content-Type" "application/json; charset=utf-8"
                             "Authorization" api-key}}))
        (.then (fn [resp] (.json resp)))
        (.then (fn [resp]
                 (let [data  (-> resp
                                 (js->clj :keywordize-keys true))]

                   ;; (js/console.log "RESPONSE: " resp)
                   ;; (js/console.log "DATA: " data)
                   data))))))

(defn ^:export handler
  [event _ callback]
  (let [query (-> event
                  (js->clj :keywordize-keys true)
                  :queryStringParameters
                  :query)]
    (-> (search-bg-images query)
        (.then (fn [data]
                 ;; (js/console.log "RESULT: " (pr-str  data))
                 (callback
                  nil
                  (clj->js
                   {:statusCode 200
                    :headers {"content-type" "application/edn; charset=utf-8"}
                    :body (pr-str data)})))))))
