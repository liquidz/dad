(ns trattoria.reader-test
  (:require [clojure.test :as t]
            [trattoria.reader :as sut]))

(defn- read-one-task [code]
  (let [[task :as res] (sut/read-tasks code)]
    (t/is (= 1 (count res)))
    task))

(t/deftest directory-test
  (t/testing "no option"
    (t/is (= {:type :directory :path "/tmp/foo/bar" :action :create}
             (read-one-task "(directory \"/tmp/foo/bar\")"))))

  (t/testing "invalid option"
    (t/is (thrown? AssertionError
                   (read-one-task "(directory \"/tmp/foo/bar\" 123)"))))

  (t/testing "invalid action"
    (t/is (thrown? AssertionError
                   (read-one-task "(directory \"/tmp/foo/bar\" {:action :invalid})"))))

  (t/testing "mode, owner, group"
    (t/is (= {:type :directory :path "/tmp/foo" :mode "0755" :owner "bar" :group "baz" :action :create}
             (read-one-task "(directory \"/tmp/foo\" {:mode \"0755\" :owner \"bar\" :group \"baz\"})")))))

(t/deftest execute-test
  (t/testing "no cwd"
    (t/is (= {:type :execute :command "foo" :cwd nil}
             (read-one-task "(execute {:command \"foo\"})"))))

  (t/testing "cwd"
    (t/is (= {:type :execute :command "foo" :cwd "bar"}
             (read-one-task "(execute {:command \"foo\" :cwd \"bar\"})"))))

  (t/testing "invalid option"
    (t/is (thrown? AssertionError
                   (read-one-task "(execute 123)"))))

  (t/testing "no command"
    (t/is (thrown? AssertionError
                   (read-one-task "(execute {:cwd \"bar\"})")))))

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

  (t/testing "multiple packages"
    (let [tasks (sut/read-tasks "(package [\"foo\" \"bar\"])")]
      (t/is (= 2 (count tasks)))
      (t/is (= [{:type :package :name "bar" :action :install}
                {:type :package :name "foo" :action :install}]
               (sort-by :name tasks)))))

  (t/testing "multiple packages with action"
    (let [tasks (sut/read-tasks "(package [\"bar\" \"baz\"] {:action :remove})")]
      (t/is (= 2 (count tasks)))
      (t/is (= [{:type :package :name "bar" :action :remove}
                {:type :package :name "baz" :action :remove}]
               (sort-by :name tasks)))))

  (t/testing "error"
    (t/testing "invalid action"
      (t/is (thrown? AssertionError
                     (read-one-task "(package \"foo\" {:action \"remove\"})")))
      (t/is (thrown? AssertionError
                     (read-one-task "(package \"foo\" {:action :invalid})"))))))
