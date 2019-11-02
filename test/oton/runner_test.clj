(ns oton.runner-test
  (:require [clojure.test :as t]
            [oton.runner :as sut]
            [oton.test-helper :as h])
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
    (let [res (run-tasks [{:type :success-test :foo "neko" :bar "inu"}])]
      (t/is (= 1 (count res)))
      (t/is (= ["sh" "-c" "^ neko body inu $"]
               (-> res first :args))))


    (t/testing "必須キーが足りない"
      (let [res (run-tasks [{:type :success-test :foo "neko"}])]
        (t/is (= 0 (count res)))
        ; (t/is (= ["sh" "-c" "^ neko body inu $"]
        ;          (-> res first :args)))
        ))
    )


  )

; (t/deftest run-default-test
;   (t/testing "success"
;     (h/with-sh-hook hooked
;       (sut/run-default {:type :__test1__ :foo "neko" :bar "inu"})
;       (t/is (= 1 (count @hooked)))
;       (t/is (= ["sh" "-c" "^ neko body inu $"]
;                (first @hooked))))
;
;     (h/with-sh-hook hooked
;       (sut/run-default {:type :__test2__ :foo "neko" :bar "inu"})
;       (t/is (= 1 (count @hooked)))
;       (t/is (= ["sh" "-c" "^ neko body inu $"]
;                (first @hooked))))
;
;     (h/with-sh-hook hooked
;       (sut/run-default {:type :__test3__ :foo "neko" :bar "inu"})
;       (t/is (= 2 (count @hooked)))
;       (t/is (= ["sh" "-c" "^ neko body inu $"]
;                (first @hooked)
;                (second @hooked)))))
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
