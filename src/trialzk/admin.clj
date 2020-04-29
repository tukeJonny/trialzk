(ns trialzk.admin
  (:require
    [zookeeper :as zk]
    [clojure.core.async :as async :refer [go]])
  (:require [trialzk.master :refer :all]))

(defn- get-master-id [conn]
  (let [{:keys [data]} (zk/data conn master-path)]
    (-> data (String.))))

(defn- list-workers [conn]
  (zk/children conn worker-path))

(defn- list-tasks [conn]
  (zk/children conn task-path))

(defn describe [conn]
  (go
    (println "Master:")
    (if (zk/exists conn master-path)
      (let [master-id (get-master-id conn)]
        (println master-id))
      (println "None"))
    (println "Workers:")
    (doseq [worker (list-workers conn)]
      (println worker))
    (println "Tasks:")
    (doseq [task (list-tasks conn)]
      (println task))))
