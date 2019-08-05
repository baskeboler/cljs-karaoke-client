(ns user 
  (:require [shadow.cljs.devtools.api :as dapi]))

(defn cljs-repl []
  (dapi/nrepl-select :app))

