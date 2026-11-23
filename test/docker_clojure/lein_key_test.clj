(ns docker-clojure.lein-key-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [docker-clojure.lein-key :as lein-key]))

(def colons
  (str/join
   "\n"
   ["tru::1:1762870000:0:3:1:5"
    "pub:-:4096:1:D8A8243577A7859F:1684357211:::-:::scESC::::::23::0:"
    "fpr:::::::::9D13D9426A0814B3373CF5E3D8A8243577A7859F:"
    "uid:-::::1684357211::972BED54DF6E1907FE129BFA8E51BA51411F22ED::Phil Hagelberg <phil@hagelb.org>::::::::::0:"
    "sub:-:4096:1:4F1856267D44B221:1684357211::::::e::::::23:"
    "fpr:::::::::B35D39C992B539C8EF362D9D4F1856267D44B221:"]))

(deftest primary-fingerprints-test
  (testing "returns primary key fingerprints but not subkey ones"
    (is (= ["9D13D9426A0814B3373CF5E3D8A8243577A7859F"]
           (lein-key/primary-fingerprints colons))))
  (testing "returns every primary key"
    (is (= ["AAAA" "BBBB"]
           (lein-key/primary-fingerprints
            (str/join "\n" ["pub:-:::::" "fpr:::::::::AAAA:"
                            "sub:-:::::" "fpr:::::::::CCCC:"
                            "pub:-:::::" "fpr:::::::::BBBB:"]))))))

(deftest summary-test
  (testing "keeps the key-describing fields and drops import metadata"
    (is (= (str/join
            "\n"
            ["pub - D8A8243577A7859F 1684357211    scESC"
             "fpr      9D13D9426A0814B3373CF5E3D8A8243577A7859F "
             "uid -  1684357211  972BED54DF6E1907FE129BFA8E51BA51411F22ED Phil Hagelberg <phil@hagelb.org> "
             "sub - 4F1856267D44B221 1684357211    e"
             "fpr      B35D39C992B539C8EF362D9D4F1856267D44B221 "])
           (lein-key/summary colons)))))
