(ns dad.runner-test
  (:require
   [clojure.test :as t]
   [dad.runner :as sut]
   [dad.runner.impl :as d.r.impl]
   [dad.test-helper :as h])
  (:import
   clojure.lang.ExceptionInfo))

(def ^:private config
  (h/read-test-config))

(def ^:private run-tasks
  (partial sut/run-tasks config))

(t/deftest succeeded?-test
  (t/are [expected input] (= expected (#'sut/succeeded? input))
    true,  {:exit 0}
    true,  [{:exit 0}]
    true,  [{:exit 0} {:exit 0}]
    true,  nil
    true,  []
    false, {:exit 1}
    false, [{:exit 1}]
    false, [{:exit 0} {:exit 1}]
    false, [{:exit 1} {:exit 1}]))

(t/deftest expand-pre-tasks-test
  (let [test-fn #(#'sut/expand-pre-tasks config %)]
    (t/testing "raw command"
      (t/is (= [{:type :expand-pre-raw-test :__def__ {:command "raw"}}]
               (test-fn {:type :expand-pre-raw-test}))))

    (t/testing "keyword"
      (t/is (= [{:type :dummy :__def__ {:command "dummy command"}}]
               (test-fn {:type :expand-pre-kw-test}))))

    (t/is (nil? (test-fn {:type :dummy})))))

(t/deftest expand-tasks-test
  (let [expand-tasks #(->> %
                           (d.r.impl/dispatch-task)
                           (#'sut/expand-tasks config))]
    (t/is (= [{:type :foo-test :__def__ {:command "foo %foo%" :requires #{:foo}}}
              {:type :bar-test :__def__ {:command "bar %bar%" :requires #{:bar}}}]
             (expand-tasks [{:type :multi-ref-test}])))

    (with-redefs [sut/expand-pre-tasks (constantly nil)]
      (t/is (= [{:type :template-create :path "foo" :source "bar"}
                {:type :chmod :path "foo" :source "bar"}
                {:type :chown :path "foo" :source "bar"}
                {:type :chgrp :path "foo" :source "bar"}]
               (map #(dissoc % :__def__)
                    (expand-tasks [{:type :template :path "foo" :source "bar"}])))))

    (t/testing "expand with successful pre"
      (h/with-test-sh true
        (t/is (= [{:type :once-test :__def__ {:command "only once" :once? true}}
                  {:type :expand-pre-raw-test :__def__ {:command "pre-raw" :pre ["raw"]}}]
                 (expand-tasks [{:type :expand-pre-raw-test}])))))

    (t/testing "expand with failing pre"
      (h/with-test-sh false
        (t/is (= [{:not-runnable? true :type :expand-pre-raw-test}]
                 (expand-tasks [{:type :expand-pre-raw-test}])))))))

(t/deftest has-enough-params?-test
  (let [expanded-task {:type :dummy
                       :__def__ {:command "%foo% %bar%"
                                 :requires #{:foo :bar}}}]
    (t/are [expected input] (= expected (#'sut/has-enough-params? input))
      false expanded-task
      false (assoc expanded-task :foo "neko")
      true  (assoc expanded-task :foo "neko" :bar "inu")
      true  (assoc expanded-task :foo "neko" :bar "inu" :baz "wani"))))

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
  (h/with-test-sh true
    (let [res (run-tasks [{:type :foo-once-test :foo "hello"}
                          {:type :bar-once-test :bar "world"}])]
      (t/is (= 3 (count res)))
      (t/is (= [["sh" "-c" "only once"]
                ["sh" "-c" "foo hello"]
                ["sh" "-c" "bar world"]]
               (map :args res))))))

(t/deftest run-tasks-pre-and-pre-not-test
  (h/with-test-sh true
    (t/testing "pre"
      ;; NOTE: foo-test will succeed always when `:foo` parameter exists
      (t/is (= [["sh" "-c" "pre"]] (map :args (run-tasks [{:type :pre-test :foo "hello"}]))))
      (t/is (= [] (map :args (run-tasks [{:type :pre-test}])))))

    (t/testing "pre-not"
      ;; NOTE: bar-test will succeed always when `:bar` parameter exists
      (t/is (= [] (map :args (run-tasks [{:type :pre-not-test :bar "world"}]))))
      (t/is (= [["sh" "-c" "pre not"]] (map :args (run-tasks [{:type :pre-not-test}])))))))

(t/deftest run-tasks-failure-test
  (h/with-test-sh true
    (t/testing "unknown command"
      (t/is (thrown-with-msg? ExceptionInfo #"Unknown command type"
              (nil? (run-tasks [{:type :unknown}]))))))

  (t/testing "failed to run command"
    (h/with-test-sh false
      (t/is (thrown-with-msg? ExceptionInfo #"Failed to run command"
              (nil? (run-tasks [{:type :foo-test :foo "hello"}])))))))
