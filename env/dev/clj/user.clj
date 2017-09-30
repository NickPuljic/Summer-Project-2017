(ns user
  (:require [mount.core :as mount]
            [nuenen.figwheel :refer [start-fw stop-fw cljs]]
            nuenen.core))

(defn start []
  (mount/start-without #'nuenen.core/repl-server))

(defn stop []
  (mount/stop-except #'nuenen.core/repl-server))

(defn restart []
  (stop)
  (start))


