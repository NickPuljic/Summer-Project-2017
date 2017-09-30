(ns ^:figwheel-no-load nuenen.app
  (:require [nuenen.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
