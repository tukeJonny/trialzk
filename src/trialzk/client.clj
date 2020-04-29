(ns trialzk.client
  (:require
   [clojure.core.async :as async :refer [go-loop <! timeout]]
   [clojure.tools.logging :as log]
   [zookeeper :as zk])
  (:require [trialzk.task :refer :all]))

(defn run-client [conn]
  (go-loop [idx 0]
    (let [command (str "exec task-" idx)
          task (make-task command)
          serialized (serialize-task task)]
      (try
        (zk/create conn "/tasks/task-" :data serialized :sequential? true :persistent? true)
        (catch Exception e (prn "caught Exception while creating task ZNode: " (.getMessage e))))
      (log/info "Created task " task)
      (<! (timeout 1000))
      (recur (inc idx)))))