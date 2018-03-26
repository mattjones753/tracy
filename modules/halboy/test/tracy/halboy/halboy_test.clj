(ns tracy.halboy.halboy-test
  (:require [clojure.test :refer :all]
            [tracy.halboy.halboy :as traced-navigator]
            [tracy.core :as core]
            [org.httpkit.fake :refer :all]
            [halboy.navigator :as navigator])
  (:import [java.util UUID]))

(defn uuid [] (str (UUID/randomUUID)))

(deftest tracing-with-contexts
  (let [discovery-address "http://example.com"
        correlation-id (uuid)]
    (with-fake-http
      [(fn [req]
         (and
           (= discovery-address (:url req))
           (= correlation-id (get (:headers req) "correlation-id"))))
       (str
         "{
           \"_links\":
             {
               \"self\": {\"href\": \"" discovery-address "\"},
               \"resource\": {\"href\": \"" discovery-address "\"}
             }
           }")]

      (testing "correlation-id is passed through discover"
        (core/traced-with-context
          {:correlation-id correlation-id}
          []
          (let [navigator-result (traced-navigator/discover discovery-address)]
            (is (= (navigator/status navigator-result) 200)))))

      (testing "correlation-id is passed through resume"
        (core/traced-with-context
          {:correlation-id correlation-id}
          []
          (let [navigator-result (traced-navigator/discover discovery-address)
                navigator-resource (navigator/resource navigator-result)
                resume-result (traced-navigator/resume navigator-resource)
                resource-result (navigator/get resume-result :resource)]
            (is (= (navigator/status resource-result) 200))))))))
