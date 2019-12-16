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
    (t/testing "default pre/postfix"
      (t/are [expected input] (= expected (sut/expand-map-to-str input m))
        "hello bar",       "hello %foo%"
        "hello bar baz",   "hello %foo% %bar%"
        "hello bar bar",   "hello %foo% %foo%"
        "hello %unknown%", "hello %unknown%"))

    (t/testing "custom pre/postfix"
      (t/are [expected input] (= expected (sut/expand-map-to-str input m "{{" "}}"))
        "hello bar",         "hello {{foo}}"
        "hello bar baz",     "hello {{foo}} {{bar}}"
        "hello bar bar",     "hello {{foo}} {{foo}}"
        "hello {{unknown}}", "hello {{unknown}}"))))

(t/deftest sha256-test
  ;; echo -n hello | sha256sum
  (t/is (= "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
           (sut/sha256 "hello"))))
