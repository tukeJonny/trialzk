(ns trialzk.worker
  (:require
   [clojure.core.async :as async :refer [go-loop <! timeout]]
   [clojure.tools.logging :as log]
   [zookeeper :as zk])
  (:import [org.apache.zookeeper KeeperException$Code])
  (:require [trialzk.master :refer [worker-path]]))

(def ^:private worker-id
  (-> (java.util.UUID/randomUUID)
      (.toString)))

(def ^:private worker-status (atom nil))

(def ^:private worker-status-idle "Idle")

(defn- worker-status-working-on [task-name]
  (str "Working on " task-name))

(defn- initiate-worker-handler [result]
  (let [code (:return-code result)
        code-str (KeeperException$Code/get code)
        path (:path result)
        ctx (:context result)]
    (condp = code
      (.intValue KeeperException$Code/CONNECTIONLOSS) (do
                                                        (log/warn "`Connection loss` occured while initiating. recreating ...")
                                                        (zk/create path :context ctx))
      (.intValue KeeperException$Code/NODEEXISTS) (log/info "ZNode" path "is already exists. ignore.")
      (.intValue KeeperException$Code/OK) (log/info "Registered successfully with " path)
      (throw (ex-info "We cannot handle error while bootstrapping" {:code code-str :path path :ctx ctx})))))

(defn- initiate-worker [conn]
  (let [worker-id-bytes (-> worker-id (.getBytes))
        path (str worker-path "/" worker-id)]
    (if-let [worker-path-exists? (zk/exists conn worker-path)]
      (zk/create conn path :data worker-id-bytes)
      (log/fatal "worker-path not found!"))))

(defn- update-worker-status-handler [conn data current-version result]
  (let [code (:return-code result)
        path (:path result)
        callback (partial update-worker-status-handler conn data current-version)]
    (if (= code (.intValue KeeperException$Code/CONNECTIONLOSS))
      (do
        (log/warn "`Connection loss` occured while initiating. retrying ...")
        (if (= (.getBytes @worker-status) data)
          (zk/set-data conn path data current-version :async? true :callback callback)
          (log/warn "Inconsistent status " @worker-status " vs " data ". retired this operation."))
        (log/trace "worker status updated to " data)))))

(defn- update-worker-status [conn status]
  (let [path (str worker-path "/" worker-id)
        new-data (.getBytes status)
        callback (partial update-worker-status-handler conn new-data)]
    (reset! worker-status status)
    (if-let [exists-result (zk/exists conn path)]
      (let [current-version (:version exists-result)]
        (zk/set-data conn path new-data current-version :async? true :callback (partial callback current-version)))
      (throw (ex-info "Failed to update worker status" {:status status})))))

(defn run-worker [conn]
  (initiate-worker conn)
  (go-loop []
    (log/info "I am a worker")
    (update-worker-status conn worker-status-idle)
    (<! (timeout 1000))
    (recur)))