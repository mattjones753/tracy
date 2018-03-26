(defproject tracy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-modules "0.3.1"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.eclipse.jetty/jetty-server "9.2.21.v20170120"]]
  :modules {:dirs ["." "modules/jetty" "modules/halboy"]})
