(ns trattoria.reader-test
  (:require [clojure.test :as t]
            [trattoria.reader :as sut]))

(defn- read-one-task [code]
  (let [[task :as res] (sut/read-tasks code)]
    (t/is (= 1 (count res)))
    task))

(t/deftest git-test
  (t/testing "no revision"
    (t/is (= {:type :git :url "foo" :path "bar" :revision "master"}
             (read-one-task "(git {:url \"foo\" :path \"bar\"})"))))

  (t/testing "revision"
    (t/is (= {:type :git :url "foo" :path "bar" :revision "baz"}
             (read-one-task "(git {:url \"foo\" :path \"bar\" :revision \"baz\"})"))))

  (t/testing "error"
    (t/testing "no url"
      (t/is (thrown? AssertionError
                     (sut/read-tasks "(git {:_url_ \"foo\" :path \"bar\"})"))))

    (t/testing "no path"
      (t/is (thrown? AssertionError
                     (sut/read-tasks "(git {:url \"foo\" :_path_ \"bar\"})"))))))

(t/deftest package-test
  (t/testing "no action"
    (t/is (= {:type :package :name "foo" :action :install}
             (read-one-task "(package \"foo\")"))))

  (t/testing "action"
    (t/is (= {:type :package :name "foo" :action :remove}
             (read-one-task "(package \"foo\" {:action :remove})"))))

  (t/testing "error"
    (t/testing "invalid action"
      (t/is (thrown? AssertionError
                     (read-one-task "(package \"foo\" {:action \"remove\"})")))
      (t/is (thrown? AssertionError
                     (read-one-task "(package \"foo\" {:action :invalid})"))))))
