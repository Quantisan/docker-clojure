(ns docker-clojure.test-runner
  (:require [clojure.test :refer :all])
  (:gen-class))

(defn -main [& namespaces]
  (doseq [ns namespaces]
    (require ns))
  (let [test-results (apply run-tests namespaces)
        {:keys [fail error]} test-results
        failures-and-errors (+ fail error)]
    (System/exit failures-and-errors)))

