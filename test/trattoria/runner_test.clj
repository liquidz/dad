(ns trattoria.runner-test
  (:require [clojure.java.shell :as sh]
            [clojure.test :as t]
            [trattoria.os :as t.os]
            [trattoria.runner :as sut])
  (:import clojure.lang.ExceptionInfo))

(defmacro with-sh-hook [hooked-atom-sym & body]
  `(let [~hooked-atom-sym (atom [])]
     (with-redefs [t.os/os-type ::testing
                   sh/sh (fn [& args#]
                           (swap! ~hooked-atom-sym conj args#)
                           {:exit 0})]
       ~@body)))

(t/deftest run-default-test
  (t/testing "success"
    (with-sh-hook hooked
      (sut/run-default {:type :__test1__ :foo "neko" :bar "inu"})
      (t/is (= 1 (count @hooked)))
      (t/is (= ["sh" "-c" "^ neko body inu $"]
               (first @hooked))))

    (with-sh-hook hooked
      (sut/run-default {:type :__test2__ :foo "neko" :bar "inu"})
      (t/is (= 1 (count @hooked)))
      (t/is (= ["sh" "-c" "^ neko body inu $"]
               (first @hooked))))

    (with-sh-hook hooked
      (sut/run-default {:type :__test3__ :foo "neko" :bar "inu"})
      (t/is (= 2 (count @hooked)))
      (t/is (= ["sh" "-c" "^ neko body inu $"]
               (first @hooked)
               (second @hooked)))))

  (t/testing "failure"
    (with-redefs [t.os/os-type ::testing
                  sh/sh (constantly {:exit 1})]
      (t/is (thrown-with-msg?
             ExceptionInfo #"Failed to find command"
             (sut/run-default {:type ::unknown})))

      (t/is (thrown-with-msg?
             ExceptionInfo #"Failed to run command"
             (sut/run-default {:type :__test1__}))))))

(t/deftest run-default-with-once-option-test
  (t/testing "no once?"
    (with-sh-hook hooked
      (sut/run-tasks []) ; clear sut/run-commands
      (dotimes [_ 10]
        (sut/run-default {:type :__test1__}))
      (t/is (= 10 (count @hooked)))))

  (t/testing "once?"
    (with-sh-hook hooked
      (sut/run-tasks []) ; clear sut/run-commands
      (dotimes [_ 10]
        (sut/run-default {:type :__once-test__}))
      (t/is (= 1 (count @hooked))))))
