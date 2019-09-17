(ns trattoria.util-test
  (:require [clojure.test :as t]
            [trattoria.util :as sut]))

(t/deftest distinct-by-test
  (let [test-data [{:a 1 :b 2}
                   {:a 2 :b 3}
                   {:a 3 :b 4}
                   {:a 2 :b 5}]]
    (t/is (= [{:a 1 :b 2}
              {:a 2 :b 3}
              {:a 3 :b 4}]
             (sut/distinct-by :a test-data)))

    (t/is (= test-data
             (sut/distinct-by :b test-data)))))
