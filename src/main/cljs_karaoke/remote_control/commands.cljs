(ns cljs-karaoke.remote-control.commands)


(defn ^export play-song-command [song]
  {:command :play-song
   :song song})

(defn ^export stop-command []
  {:command :stop})

(defn ^export playlist-next-command []
  {:command :playlist-next})
