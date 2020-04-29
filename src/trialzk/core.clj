(ns trialzk.core
  (:require [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clojure.core.async :as async :refer [<!! close!]]
            [clj-sub-command.core :refer [parse-cmds]]
            [zookeeper :as zk])
  (:require [trialzk.master :refer :all]
            [trialzk.worker :refer :all]
            [trialzk.client :refer :all]
            [trialzk.admin :refer :all]
            [trialzk.watcher :refer :all]))

(defn show-help [opt-summary cmd-summary]
  (string/join \newline
               ["trialzk [--help] [run-master|run-worker]"
                ""
                "Options:"
                opt-summary
                ""
                "Commands:"
                cmd-summary]))

(defn show-errs [errs]
  (str "Error:\n\n" (string/join \newline errs)))

(def options
  [["-h" "--help"]])

(def commands
  [["run-master" "Run master node"]
   ["run-worker" "Run worker node"]
   ["run-client" "Run client"]
   ["describe" "Show all resources"]])

(defn- run [conn command]
  (case command
    :run-client (run-client conn)
    :run-master (run-master conn)
    :run-worker (run-worker conn)
    :describe (describe conn)))

(defn on-shutdown [conn shutdown-ch]
  (log/warn "Shutdown trialzk ...")
  (close! shutdown-ch)
  (zk/close conn))

(defn -main [& args]
  (let [{:keys [options command arguments errors options-summary commands-summary]} (parse-cmds args options commands)]
    (cond
      (:help options) ((println (show-help options-summary commands-summary))
                       (System/exit 0))
      errors ((println (show-errs errors))
              (System/exit 1)))
    (let [runtime (Runtime/getRuntime)
          conn (zk/connect "127.0.0.1:2181" :watcher watch-event)
          shutdown-ch (run conn command)]
      (.addShutdownHook runtime (Thread. (partial on-shutdown conn shutdown-ch)))
      ; FIXME: wait for on-shutdown
      (<!! shutdown-ch))))
