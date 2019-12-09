(ns dad.util-test
  (:require [clojure.test :as t]
            [dad.util :as sut]))

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

(t/deftest ensure-seq-test
  (t/is (= [1] (sut/ensure-seq 1)))
  (t/is (= [1] (sut/ensure-seq [1])))
  (t/is (= [nil] (sut/ensure-seq nil))))

(t/deftest expand-map-to-str-test
  (let [m {:foo "bar" :bar "baz"}]
    (t/are [expected input] (= expected (sut/expand-map-to-str input m))
      "hello bar",       "hello %foo%"
      "hello bar baz",   "hello %foo% %bar%"
      "hello bar bar",   "hello %foo% %foo%"
      "hello %unknown%", "hello %unknown%")))
