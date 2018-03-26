(ns tracy.jetty.jetty-extractor-test
  (:require [clojure.test :refer :all]
            [tracy.core :as trace]
            [tracy.jetty.jetty-extractor :as trace-jetty])
  (:import [java.util UUID]
           [org.eclipse.jetty.server.handler HandlerWrapper]
           [javax.servlet.http HttpServletRequest]))

(def ^:dynamic a-variable "initial-value")

(defn uuid [] (str (UUID/randomUUID)))

(defn stub-request
  [headers]
  (proxy [HttpServletRequest] []
    (getHeader [headerName]
      (get headers headerName))))

(testing "tracing jetty handler should wrap request handling with context"
  (binding [a-variable "test"]
    (let [expected-correlation-id (str (str (UUID/randomUUID)))
          handler ^HandlerWrapper (trace-jetty/tracing-handler-factory
                                    [(fn [f]
                                       (set!
                                         a-variable
                                         (:correlation-id-key
                                           (trace/get-tracing-context)))
                                       (f))]
                                    {:correlation-id-key uuid})
          request-headers {"correlation-id-key" expected-correlation-id}
          _ (.handle handler "" nil (stub-request request-headers) nil)]
      (is (= expected-correlation-id a-variable)))))
