(ns docker-clojure.util
  (:require [docker-clojure.core :as-alias core]))

(defn get-or-default
  "Returns the value in map m for key k or else the value for key :default."
  [m k]
  (get m k (get m :default)))
