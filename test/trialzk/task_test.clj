(ns trialzk.task-test
  (:require [clojure.test :refer :all]
            [trialzk.task :refer :all]))

(deftest serialize-test
  (testing "deserialize serialized task"
    (let [task (make-task "ls /")
          serialized (serialize-task task)
          deserialized (deserialize-task serialized)]
      (is (= task deserialized)))))
