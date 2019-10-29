(ns trattoria.reader-test
  (:require [clojure.test :as t]
            [trattoria.reader :as sut]
            [clojure.string :as str])
  (:import clojure.lang.ExceptionInfo))

(defn- read-one-task [code]
  (let [[task :as res] (sut/read-tasks code)]
    (t/is (= 1 (count res)))
    (dissoc task :id)))

(t/deftest task-id-test
  (let [[{:keys [id]}] (sut/read-tasks "(package \"foo\")")]
    (t/is (and (string? id) (not (str/blank? id))))))

(t/deftest distinct-by-task-id-test
  (t/testing "duplicated tasks"
    (let [tasks (sut/read-tasks "(package [\"foo\" \"bar\"])
                                 (package \"foo\")")]
      (t/is (= 2 (count tasks)))
      (t/is (= [{:type :package :name "bar" :action :install}
                {:type :package :name "foo" :action :install}]
               (->> tasks
                    (sort-by :name)
                    (map #(dissoc % :id)))))))

  (t/testing "different action tasks"
    (let [tasks (sut/read-tasks "(package [\"foo\" \"bar\"])
                                         (package \"foo\" {:action :remove})")]
      (t/is (= 3 (count tasks)))
      (t/is (= [{:type :package :name "bar" :action :install}
                {:type :package :name "foo" :action :install}
                {:type :package :name "foo" :action :remove}]
               (->> tasks
                    (sort-by #(str (:name %) (:action %)))
                    (map #(dissoc % :id))))))))

(t/deftest directory-test
  (t/testing "only resource name"
    (t/is (= {:type :directory :path "/tmp/foo/bar" :action :create}
             (read-one-task "(directory \"/tmp/foo/bar\")"))))

  (t/testing "no resource name"
    (t/is (= {:type :directory :path "/tmp/foo/bar" :action :create}
             (read-one-task "(directory {:path \"/tmp/foo/bar\"})"))))

  (t/testing "invalid option"
    (t/is (thrown? ExceptionInfo
                   (read-one-task "(directory \"/tmp/foo/bar\" 123)"))))

  (t/testing "invalid action"
    (t/is (thrown? ExceptionInfo
                   (read-one-task "(directory \"/tmp/foo/bar\" {:action :invalid})"))))


  (t/testing "mode, owner, group"
    (t/is (= {:type :directory :path "/tmp/foo" :mode "0755" :owner "bar" :group "baz" :action :create}
             (read-one-task "(directory \"/tmp/foo\" {:mode \"0755\" :owner \"bar\" :group \"baz\"})")))))

(t/deftest execute-test
  (t/testing "only resource name"
    (t/is (= {:type :execute :command "foo" :cwd nil}
             (read-one-task "(execute \"foo\")"))))

  (t/testing "no resource name"
    (t/is (= {:type :execute :command "foo" :cwd nil}
             (read-one-task "(execute {:command \"foo\"})"))))

  (t/testing "cwd"
    (t/is (= {:type :execute :command "foo" :cwd "bar"}
             (read-one-task "(execute {:command \"foo\" :cwd \"bar\"})"))))

  (t/testing "invalid option"
    (t/is (thrown? ExceptionInfo
                   (read-one-task "(execute 123)"))))

  (t/testing "no command"
    (t/is (thrown? ExceptionInfo
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
      (t/is (thrown? ExceptionInfo
                     (sut/read-tasks "(git {:_url_ \"foo\" :path \"bar\"})"))))

    (t/testing "no path"
      (t/is (thrown? ExceptionInfo
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
               (->> tasks
                    (sort-by :name)
                    (map #(dissoc % :id)))))))

  (t/testing "multiple packages with action"
    (let [tasks (sut/read-tasks "(package [\"bar\" \"baz\"] {:action :remove})")]
      (t/is (= 2 (count tasks)))
      (t/is (= [{:type :package :name "bar" :action :remove}
                {:type :package :name "baz" :action :remove}]
               (->> tasks
                    (sort-by :name)
                    (map #(dissoc % :id)))))))

  (t/testing "error"
    (t/testing "invalid action"
      (t/is (thrown? ExceptionInfo
                     (read-one-task "(package \"foo\" {:action \"remove\"})")))
      (t/is (thrown? ExceptionInfo
                     (read-one-task "(package \"foo\" {:action :invalid})"))))))
