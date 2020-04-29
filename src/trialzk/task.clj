(ns trialzk.task
  (:require [deercreeklabs.lancaster :as l]))

(l/def-record-schema task-schema
  [:id l/string-schema]
  [:command l/string-schema])

(defn make-task [command]
  (let [uuid (-> (java.util.UUID/randomUUID) (.toString))]
    {:id uuid
     :command command}))

(defn serialize-task [task]
  (l/serialize task-schema task))

(defn deserialize-task [encoded]
  (l/deserialize task-schema task-schema encoded))
