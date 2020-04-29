(defproject trialzk "0.1.0"
  :description "ZooKeeper trial"
  :url "https://github.com/tukeJonny/trialzk"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
    [org.clojure/clojure "1.10.0"]
    [zookeeper-clj "0.9.4"]
    [clj-sub-command "0.6.0"]
    [deercreeklabs/lancaster "0.9.2"]
    [org.clojure/core.async "1.1.587"]
    [org.clojure/tools.logging "1.1.0"]
  ]
  :main trialzk.core
  :repl-options {:init-ns trialzk.core})
