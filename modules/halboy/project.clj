(defproject tracy.halboy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :plugins [[lein-modules "0.3.11"]]
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [tracy :version]
                 [halboy "3.0.0"]]
  :profiles {:shared {:dependencies
                    [[http-kit.fake "0.2.2"]]}
             :test [:shared]})
