(ns cljs-karaoke.embed
  (:require [pl.danieljanus.tagsoup :as ts]))

(defmacro inline-svg [file]
  (let [file `(slurp file)]
    `(ts/parse-string file)))

(comment
  (inline-svg "public/images/logo-2.svg"))
