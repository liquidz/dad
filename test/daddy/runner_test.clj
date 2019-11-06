(ns daddy.runner-test
  (:require [clojure.test :as t]
            [daddy.runner :as sut]
            [daddy.test-helper :as h])
  (:import clojure.lang.ExceptionInfo))

(def ^:private run-tasks
  (partial sut/run-tasks (h/read-test-config)))

(t/deftest succeeded?-test
  (t/are [expected input] (= expected (sut/succeeded? input))
    true,  {:exit 0}
    true,  [{:exit 0}]
    true,  [{:exit 0} {:exit 0}]
    true,  nil
    true,  []
    false, {:exit 1}
    false, [{:exit 1}]
    false, [{:exit 0} {:exit 1}]
    false, [{:exit 1} {:exit 1}]))

(t/deftest run-tasks-test
  (h/with-test-sh true
    (t/testing "success"
      (let [res (run-tasks [{:type :success-test :foo "neko" :bar "inu"}])]
        (t/is (= 1 (count res)))
        (t/is (= ["sh" "-c" "^ neko body inu $"]
                 (-> res first :args)))))

    (t/testing "missing required keys"
      (let [res (run-tasks [{:type :success-test :foo "neko"}])]
        (t/is (= 0 (count res)))))

    (t/testing "one ref"
      (let [res (run-tasks [{:type :one-ref-test :foo "one" :bar "ref"}])]
        (t/is (= 1 (count res)))
        (t/is (= ["sh" "-c" "^ one body ref $"]
                 (-> res first :args)))))

    (t/testing "multi ref"
      (let [res (run-tasks [{:type :multi-ref-test :foo "multi" :bar "ref"}])]
        (t/is (= 2 (count res)))
        (t/is (= [["sh" "-c" "foo multi"]
                  ["sh" "-c" "bar ref"]]
                 (map :args res))))

      (let [res (run-tasks [{:type :multi-ref-test :foo "multi"}])]
        (t/is (= 1 (count res)))
        (t/is (= ["sh" "-c" "foo multi"]
                 (-> res first :args))))

      (let [res (run-tasks [{:type :multi-ref-test :bar "ref"}])]
        (t/is (= 1 (count res)))
        (t/is (= ["sh" "-c" "bar ref"]
                 (-> res first :args)))))))


(t/deftest run-tasks-once-test
  (h/with-test-sh
    (run-tasks [{:type :foo-once-test :foo "hello"}
                {:type :bar-once-test :bar "world"}
                ])
    )
  )
; (t/deftest run-default-test
;   (t/testing "success"
;
;
;   (t/testing "failure"
;     (h/with-fail-sh
;       (t/is (thrown-with-msg?
;              ExceptionInfo #"Failed to find command"
;              (sut/run-default {:type ::unknown})))
;
;       (t/is (thrown-with-msg?
;              ExceptionInfo #"Failed to run command"
;              (sut/run-default {:type :__test1__}))))))
;
; (t/deftest run-default-with-once-option-test
;   (t/testing "no once?"
;     (h/with-sh-hook hooked
;       (sut/run-tasks []) ; clear sut/run-commands
;       (dotimes [_ 10]
;         (sut/run-default {:type :__test1__}))
;       (t/is (= 10 (count @hooked)))))
;
;   (t/testing "once?"
;     (h/with-sh-hook hooked
;       (sut/run-tasks []) ; clear sut/run-commands
;       (dotimes [_ 10]
;         (sut/run-default {:type :__once-test__}))
;       (t/is (= 1 (count @hooked))))))
