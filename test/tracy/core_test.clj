(ns tracy.core-test
  (:require [clojure.test :refer :all]
            [tracy.core :as tracing]))

(def ^:dynamic a-variable "initial-value")

(defn uuid?
  [value]
  (re-matches #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" value))

(let [trace-id-value (tracing/uuid)
      parent-span-id-value (tracing/uuid)]
  (deftest tracing-with-contexts
    (testing "trace id and span id are used when one is set in context"
      (tracing/traced
        trace-id-value parent-span-id-value
        []
        (is (= trace-id-value (:trace-id (tracing/get-tracing-context))))
        (is (uuid? (:span-id (tracing/get-tracing-context))))))

    (testing "uuid trace id is created when one is not set"
      (tracing/traced nil nil [] (is (uuid? (:trace-id (tracing/get-tracing-context))))))


    (testing "no parent span id is created when span is not set"
      (tracing/traced nil nil [] (is (nil? (:parent-span-id (tracing/get-tracing-context))))))

    (testing "handles nil interceptors"
      (tracing/traced nil nil nil (is (uuid? (:trace-id (tracing/get-tracing-context))))))

    (testing "span tags are not propagated to sub-spans"
      (tracing/traced
        trace-id-value parent-span-id-value
        []
        (tracing/add-tags! {:key "value"})
        (is (= {:key "value"} (:tags (tracing/get-tracing-context))))
        (tracing/traced-inherited
          (is (= {} (:tags (tracing/get-tracing-context)))))))

    (testing "new span ids are generated for sub-spans and previous set to parent-span-id"
      (tracing/traced
        trace-id-value parent-span-id-value []
        (is (= parent-span-id-value (:parent-span-id (tracing/get-tracing-context))))
        (let [outer-span-id (:span-id (tracing/get-tracing-context))]
          (tracing/traced-inherited
            (is (not (= parent-span-id-value (:span-id (tracing/get-tracing-context)))))
            (is (uuid? (:span-id (tracing/get-tracing-context))))
            (is (= outer-span-id (:parent-span-id (tracing/get-tracing-context))))))))

    (testing "inherited with nothing to inherit uses new ids and empty parent"
      (is (= {} (tracing/get-tracing-context)))
      (tracing/traced-inherited
        (is (uuid? (:trace-id (tracing/get-tracing-context))))
        (is (uuid? (:span-id (tracing/get-tracing-context))))
        (is (nil? (:parent-span-id (tracing/get-tracing-context))))))

    (testing "trace ids are propagated to sub-spans"
      (tracing/traced
        trace-id-value nil []
        (is (= trace-id-value (:trace-id (tracing/get-tracing-context))))
        (tracing/traced-inherited
          (is (= trace-id-value (:trace-id (tracing/get-tracing-context)))))))

    (testing "context is cleared on error"
      (try
        (tracing/traced
          trace-id-value nil
          []
          (is (= trace-id-value (:trace-id (tracing/get-tracing-context))))
          (throw (Exception.)))
        (catch Exception _))
      (is (nil? (:trace-id (tracing/get-tracing-context)))))

    (testing "tracing interceptors should be executed as part of trace when provided"
      (tracing/traced
        trace-id-value nil
        [(fn [f]
           (binding [a-variable (:trace-id (tracing/get-tracing-context))] (f)))]
        (is (= trace-id-value a-variable))))

    (testing "tracing interceptors should be executed as part of inherited trace when provided"
      (binding [a-variable 1]
        (tracing/traced
          trace-id-value nil
          [(fn [f]
             (binding [a-variable (+ a-variable 1)] (f)))]
          (is (= 2 a-variable))
          (tracing/traced-inherited
            (is (= 3 a-variable))))))))
