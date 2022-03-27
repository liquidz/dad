(ns dad.reader-test
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [dad.constant :as d.const]
   [dad.reader :as sut]
   [dad.test-helper :as h])
  (:import
   clojure.lang.ExceptionInfo))

(def ^:private test-config (h/read-test-config))

(defn- read-tasks
  [code]
  (->> code
       (str)
       (sut/read-tasks test-config)
       (:tasks)))

(defn- read-one-task
  [code]
  (let [[task :as res] (read-tasks code)]
    (t/is (= 1 (count res)))
    (dissoc task :id)))

(defn- task-name
  [x]
  (symbol (str d.const/pod-name) (str x)))

(t/deftest task-id-test
  (let [[{:keys [id]}] (read-tasks `(~(task-name "package") {:name "foo"}))]
    (t/is (and (string? id) (not (str/blank? id))))))

(t/deftest distinct-by-task-id-test
  (t/testing "duplicated tasks"
    (let [tasks (read-tasks (str "(" (task-name "package") " {:name [\"foo\" \"bar\"]})
                                  (" (task-name "package") " {:name \"foo\"})"))]
      (t/is (= 2 (count tasks)))
      (t/is (= [{:type :package :name "bar" :action :install}
                {:type :package :name "foo" :action :install}]
               (->> tasks
                    (sort-by :name)
                    (map #(dissoc % :id)))))))

  (t/testing "different action tasks"
    (let [tasks (read-tasks (str "(" (task-name "package") " {:name [\"foo\" \"bar\"]})
                                  (" (task-name "package") " {:name \"foo\" :action :remove})"))]
      (t/is (= 3 (count tasks)))
      (t/is (= [{:type :package :name "bar" :action :install}
                {:type :package :name "foo" :action :install}
                {:type :package :name "foo" :action :remove}]
               (->> tasks
                    (sort-by #(str (:name %) (:action %)))
                    (map #(dissoc % :id))))))))

(t/deftest directory-test
  (let [directory (task-name "directory")]
    (t/testing "required arguments"
      (t/is (= {:type :directory :path "/tmp/foo/bar" :action :create}
               (read-one-task `(~directory {:path "/tmp/foo/bar"})))))

    (t/testing "string action"
      (t/is (= {:type :directory :path "/tmp/foo/bar" :action :remove}
               (read-one-task `(~directory {:path "/tmp/foo/bar" :action "remove"})))))

    (t/testing "empty path"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~directory {:path ""})))))

    (t/testing "invalid arguments"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~directory 123)))))

    (t/testing "invalid action"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~directory {:path "/tmp/foo/bar" :action :invalid})))))

    (t/testing "mode, owner, group"
      (t/is (= {:type :directory :path "/tmp/foo" :mode "755" :owner "bar" :group "baz" :action :create}
               (read-one-task `(~directory {:path "/tmp/foo" :mode "755" :owner "bar" :group "baz"})))))))

(t/deftest execute-test
  (let [execute (task-name "execute")]
    (t/testing "required arguments"
      (t/is (= {:type :execute :command "foo" :cwd nil}
               (read-one-task `(~execute {:command "foo"})))))

    (t/testing "cwd"
      (t/is (= {:type :execute :command "foo" :cwd "bar"}
               (read-one-task `(~execute {:command "foo" :cwd "bar"})))))

    (t/testing "pre"
      (t/is (= {:type :execute :command "foo" :pre "bar" :cwd nil}
               (read-one-task `(~execute {:command "foo" :pre "bar"}))))
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~execute {:command "foo" :pre 123})))))

    (t/testing "pre-not"
      (t/is (= {:type :execute :command "foo" :pre-not "bar" :cwd nil}
               (read-one-task `(~execute {:command "foo" :pre-not "bar"}))))
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~execute {:command "foo" :pre-not 123})))))

    (t/testing "empty command"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~execute {:command ""})))))

    (t/testing "invalid arguments"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~execute 123)))))

    (t/testing "no command"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~execute {:cwd "bar"})))))))

(t/deftest file-test
  (let [file (task-name "file")]
    (t/testing "required arguments"
      (t/is (= {:type :file :path "/tmp/foo" :action :create}
               (read-one-task `(~file {:path "/tmp/foo"})))))

    (t/testing "action"
      (t/is (= {:type :file :path "/tmp/foo" :action :delete}
               (read-one-task `(~file {:path "/tmp/foo" :action "delete"})))))

    (t/testing "no required arguments"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~file {})))))

    (t/testing "empty path"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~file {:path ""})))))

    (t/testing "invalid action"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~file {:path "/tmp/foo" :action "invalid"})))))

    (t/testing "mode, owner, group"
      (t/is (= {:type :file :path "/tmp/foo" :mode "755" :owner "bar" :group "baz" :action :create}
               (read-one-task `(~file {:path "/tmp/foo" :mode "755" :owner "bar" :group "baz"})))))))

(t/deftest git-test
  (let [git (task-name "git")]
    (t/testing "required arguments"
      (t/is (= {:type :git :url "foo" :path "bar" :revision "main"}
               (read-one-task `(~git {:url "foo" :path "bar"})))))

    (t/testing "revision"
      (t/is (= {:type :git :url "foo" :path "bar" :revision "baz"}
               (read-one-task `(~git {:url "foo" :path "bar" :revision "baz"})))))

    (t/testing "mode, owner, group"
      (t/is (= {:type :git :url "foo" :path "bar" :revision "main" :mode "644" :owner "alice" :group "baz"}
               (read-one-task `(~git {:url "foo" :path "bar" :mode "644" :owner "alice" :group "baz"})))))

    (t/testing "error"
      (t/testing "empty path and url"
        (t/is (thrown? ExceptionInfo
                (read-tasks `(~git {:url "foo" :path ""}))))
        (t/is (thrown? ExceptionInfo
                (read-tasks `(~git {:url "" :path "bar"}))))
        (t/is (thrown? ExceptionInfo
                (read-tasks `(~git {:url "" :path ""})))))

      (t/testing "no url"
        (t/is (thrown? ExceptionInfo
                (read-tasks `(~git {:path "bar"})))))

      (t/testing "no path"
        (t/is (thrown? ExceptionInfo
                (read-tasks `(~git {:url "foo"}))))))))

(t/deftest download-test
  (let [download (task-name "download")]
    (t/testing "required arguments"
      (t/is (= {:type :download :url "foo" :path "bar"}
               (read-one-task `(~download {:url "foo" :path "bar"})))))

    (t/testing "modes"
      (t/is (= {:type :download :url "foo" :path "bar" :mode "755" :owner "alice" :group "baz"}
               (read-one-task `(~download {:url "foo" :path "bar" :mode "755" :owner "alice" :group "baz"})))))

    (t/testing "empty path and url"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~download {:path "foo" :url ""}))))
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~download {:path "" :url "bar"}))))
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~download {:path "" :url ""})))))

    (t/testing "no url"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~download {:path "foo"})))))

    (t/testing "no path"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~download {:url "foo"})))))))

(t/deftest link-test
  (let [link (task-name "link")]
    (t/testing "required arguments"
      (t/is (= {:type :link :path "/tmp/foo" :source "/tmp/bar"}
               (read-one-task `(~link {:path "/tmp/foo" :source "/tmp/bar"})))))

    (t/testing "empty arguments"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~link {:path "" :source "/tmp/bar"}))))
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~link {:path "/tmp/foo" :source ""})))))

    (t/testing "no required arguments"
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~link {:path "/tmp/foo"}))))
      (t/is (thrown? ExceptionInfo
              (read-one-task `(~link {:source "/tmp/foo"})))))))

(t/deftest package-test
  (let [package (task-name "package")]
    (t/testing "required arguments"
      (t/is (= {:type :package :name "foo" :action :install}
               (read-one-task `(~package {:name "foo"})))))

    (t/testing "action"
      (t/is (= {:type :package :name "foo" :action :remove}
               (read-one-task `(~package {:name "foo" :action :remove})))))

    (t/testing "multiple packages"
      (let [tasks (read-tasks `(~package {:name ["foo" "bar"]}))]
        (t/is (= 2 (count tasks)))
        (t/is (= [{:type :package :name "bar" :action :install}
                  {:type :package :name "foo" :action :install}]
                 (->> tasks
                      (sort-by :name)
                      (map #(dissoc % :id)))))))

    (t/testing "multiple packages with action"
      (let [tasks (read-tasks `(~package {:name ["bar" "baz"] :action :remove}))]
        (t/is (= 2 (count tasks)))
        (t/is (= [{:type :package :name "bar" :action :remove}
                  {:type :package :name "baz" :action :remove}]
                 (->> tasks
                      (sort-by :name)
                      (map #(dissoc % :id)))))))

    (t/testing "error"
      (t/testing "empty name"
        (t/is (thrown? ExceptionInfo
                (read-one-task `(~package {:name ""})))))

      (t/testing "invalid action"
        (t/is (thrown? ExceptionInfo
                (read-one-task `(~package {:name "foo" :action "invalid"}))))
        (t/is (thrown? ExceptionInfo
                (read-one-task `(~package {:name "foo" :action :invalid}))))))))

(t/deftest template-test
  (let [template (task-name "template")]
    (t/testing "required arguments"
      (t/is (= {:type :template :path "foo" :source "bar"}
               (read-one-task `(~template {:path "foo" :source "bar"})))))

    (t/testing "variables"
      (t/is (= {:type :template :path "foo" :source "bar" :variables {:one 1 :two 2}}
               (read-one-task `(~template {:path "foo" :source "bar" :variables {:one 1 :two 2}})))))

    (t/testing "error"
      (t/testing "empty path and source"
        (t/is (thrown? ExceptionInfo
                (read-one-task `(~template {:path "foo" :source ""}))))
        (t/is (thrown? ExceptionInfo
                (read-one-task `(~template {:path "" :source "bar"}))))
        (t/is (thrown? ExceptionInfo
                (read-one-task `(~template {:path "" :source ""})))))

      (t/testing "no source"
        (t/is (thrown? ExceptionInfo
                (read-one-task `(~template {:path "foo"}))))))))

(t/deftest load-file-test
  (let [file (task-name "file")
        directory (task-name "directory")
        load-file* (task-name "load-file")
        dummy-loaded-content (str/join `((~file {:path (~'hello "neko")})
                                         (~directory {:path (~'hello "inu")})))
        dummy-input-code (str/join `((defn ~'hello
                                       [~'s]
                                       (str "hello " ~'s))
                                     (~load-file* "dummy.clj")))]
    (with-redefs [slurp (constantly dummy-loaded-content)]
      (t/is (= [{:type :file :path "hello neko" :action :create}
                {:type :directory :action :create :path "hello inu"}]
               (map #(dissoc % :id) (read-tasks dummy-input-code)))))))
