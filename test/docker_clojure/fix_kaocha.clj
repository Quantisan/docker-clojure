(ns docker-clojure.fix-kaocha
  "Kaocha often has bugs with tools-deps exec-fn usage because they have two
  different entry points (and one doesn't use the other). They insist on
  everyone using a `bin/kaocha` script to run their tests even though that is
  inconsistent with most Clojure tooling idioms. This ns exists to work around
  bugs we encounter and make kaocha behave better."
  (:require [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]
            [kaocha.runner :as kaocha]))

(defn run-tests
  [opts]
  ;; work around https://github.com/lambdaisland/kaocha/issues/445
  (binding [spec/*explain-out* expound/printer]
    (kaocha/exec-fn opts)))
