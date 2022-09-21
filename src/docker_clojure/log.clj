(ns docker-clojure.log
  (:require [clojure.core.async :refer [chan put! <!! close!] :as async]))

;; init w/ closed chan so that start works the same on first startup as subsequent ones
(defonce log-ch (doto (chan) close!))

(defn start []
  (when (nil? (<!! log-ch))
    (alter-var-root #'log-ch (constantly (chan 10)))
    (async/thread
      (loop []
        (when-let [msgs (<!! log-ch)]
          (apply println msgs)
          (recur))))))

(defn stop []
  (close! log-ch))

(defn log
  [& msgs]
  (let [msgs (if (nil? msgs) "" msgs)]
    (put! log-ch msgs)))
