(ns trialzk.master
  (:require
   [clojure.core.async :as async :refer [go-loop <! timeout]]
   [clojure.tools.logging :as log]
   [zookeeper :as zk])
  (:import [org.apache.zookeeper KeeperException$Code])
  (:require [trialzk.task :refer :all]))

(def ^:private master-id
  (-> (java.util.UUID/randomUUID)
      (.toString)))

(def master-path "/master")

(def ^:private assign-path "/assign")

(def worker-path "/workers")

(def task-path "/tasks")

(def status-path "/status")

(defn- get-current-master [conn]
  (let [{:keys [data]} (zk/data conn master-path)]
    (-> data (String.))))

(defn- bootstrap-handler [conn result]
  (let [code (:return-code result)
        code-str (KeeperException$Code/get code)
        path (:path result)
        callback (partial bootstrap-handler conn)]
    (condp = code
      (.intValue KeeperException$Code/CONNECTIONLOSS) (do
                                                        (log/warn "`Connection loss` occured while bootstraping. recreating ...")
                                                        (zk/create conn path :persistent? true :async? true :callback callback))
      (.intValue KeeperException$Code/NODEEXISTS) (log/info "ZNode " path " is already exists. ignore.")
      (.intValue KeeperException$Code/OK) (log/info "Bootstrap ZNode " path " completed successfully.")
      (throw (ex-info "We cannot handle error while bootstrapping" {:code code-str :path path})))))

(defn- bootstrap [conn]
  (let [callback (partial bootstrap-handler conn)]
    (zk/create conn worker-path :persistent? true :async? true :callback callback)
    (zk/create conn assign-path :persistent? true :async? true :callback callback)
    (zk/create conn task-path :persistent? true :async? true :callback   callback)
    (zk/create conn status-path :persistent? true :async? true :callback callback)))

(defn- initiate-master [conn]
  (let [master-id-bytes (-> master-id (.getBytes))]
    (if-not (zk/exists conn master-path)
      (do
        (log/info "Initiate master with master-id=" master-id "...")
        (zk/create conn master-path :data master-id-bytes))
      (let [current-master-id (get-current-master conn)]
        (log/warn "Master had already initiated with master-id=" current-master-id "...")))))

(defn- is-leader? [conn]
  (let [current-master-id (get-current-master conn)]
    (= master-id current-master-id)))

(defn run-master [conn]
  (bootstrap conn)
  (initiate-master conn)
  (go-loop []
    (if (is-leader? conn)
      (log/info "I am a leader.")
      (log/warn "I am not a leader."))
    (<! (timeout 1000))
    (recur)))