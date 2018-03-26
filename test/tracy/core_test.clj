(ns tracy.core-test
  (:require [clojure.test :refer :all]
            [tracy.core :as trace]))

(def ^:dynamic a-variable "initial-value")

(deftest tracing-with-contexts

  (testing "can see correlation-id"
    (trace/traced-with-context
      {:context-key "expected value"}
      []
      (is (= "expected value" (:context-key (trace/get-tracing-context))))))

  (testing "handles nil interceptors"
    (trace/traced-with-context
      {:context-key "expected value"}
      nil
      (is (= "expected value" (:context-key (trace/get-tracing-context))))))

  (testing "outside nested tracing form uses context defined outside"
    (trace/traced-with-context
      {:context-key "expected value"}
      []
      (is (= "expected value" (:context-key (trace/get-tracing-context))))
      (trace/traced-with-context
        {:context-key "different expected value"}
        []
        (is (=
              "different expected value"
              (:context-key (trace/get-tracing-context)))))
      (is (= "expected value" (:context-key (trace/get-tracing-context))))))

  (testing "context is cleared on error"
    (try
      (trace/traced-with-context
        {:context-key "expected value"}
        []
        (is (= "expected value" (:context-key (trace/get-tracing-context))))
        (throw (Exception. "my exception message"))
        )
      (catch Exception ignored))
    (is (nil? (:context-key (trace/get-tracing-context)))))

  (testing "tracing interceptors should be executed as
  part of trace when provided"
    (trace/traced-with-context
      {:context-key "expected value"}
      [(fn [f]
         (binding [a-variable "overriden-value"] (f)))]
      (is (= "expected value" (:context-key (trace/get-tracing-context))))
      (is (= "overriden-value" a-variable)))))
