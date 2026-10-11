(ns docker-clojure.log
  (:require [clojure.core.async :refer [chan put! <!! close!] :as async]))

;; init w/ closed chan so that start works the same on first startup as subsequent ones
(defonce log-ch (doto (chan) close!))

;; closes once the logging thread has printed everything it was sent
(defonce drained-ch (doto (chan) close!))

(defn start []
  (when (nil? (<!! log-ch))
    (alter-var-root #'log-ch (constantly (chan 10)))
    (alter-var-root
     #'drained-ch
     (constantly
      (async/thread
        (loop []
          (when-let [msgs (<!! log-ch)]
            (apply println msgs)
            (recur))))))))

(defn stop
  "Stops the logger, blocking until all pending messages have been printed."
  []
  (close! log-ch)
  (<!! drained-ch))

(defn log
  [& msgs]
  (let [msgs (if (nil? msgs) "" msgs)]
    (put! log-ch msgs)))
