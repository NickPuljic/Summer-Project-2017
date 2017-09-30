(ns nuenen.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [nuenen.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[nuenen started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[nuenen has shut down successfully]=-"))
   :middleware wrap-dev})
