(ns tracy.core
  (:import (java.util UUID)))

(defn- default-interceptor
  "The default, empty, interceptor function.  Just calls its argument."
  [main-function]
  (main-function))

(defn uuid
  []
  (.toString (UUID/randomUUID)))

(defn compose-interceptors
  "Composes two interceptor functions, creating a new interceptor function
  that combines their behavior."
  [function-1 function-2]
  (fn [main-function] (function-1 (fn [] (function-2 main-function)))))

(defn join-interceptors
  "Composes a collection of interceptors, in order. Always returns a valid
  interceptor function, even if the collection is empty."
  [interceptors]
  (reduce compose-interceptors default-interceptor interceptors))

(def ^:private thread-local-context
  (ThreadLocal.))
(def ^:private thread-local-interceptor
  (ThreadLocal.))

(defn get-tracing-context
  []
  (merge {} (.get thread-local-context)))

(defn set-tracing-context
  [m]
  (.set thread-local-context m))

(defn add-tags!
  [m]
  (let [current-context (get-tracing-context)
        current-tags (:tags current-context)
        updated-tags (merge current-tags m)]
    (set-tracing-context
      (merge current-context {:tags updated-tags}))))

(defn get-current-interceptor
  []
  (or (.get thread-local-interceptor) default-interceptor))

(defn set-current-interceptor
  [interceptor]
  (.set thread-local-interceptor interceptor))

(defn new-tracing-span
  [trace-id parent-span-id]
  (set-tracing-context {:trace-id       (or trace-id (uuid))
                        :span-id        (uuid)
                        :parent-span-id parent-span-id
                        :tags           {}})
  thread-local-context)

(defmacro traced
  [trace-id parent-span-id interceptors & body]
  `(let
     [original-context# (get-tracing-context)
      current-interceptor# (get-current-interceptor)]
     (set-current-interceptor (join-interceptors ~interceptors))
     (new-tracing-span ~trace-id ~parent-span-id)
     (try
       ((get-current-interceptor) (fn [] (do ~@body)))
       (finally
         (set-tracing-context original-context#)
         (set-current-interceptor current-interceptor#)))))

(defmacro traced
  [interceptors & body]
  `(let
     [original-context# (get-tracing-context)
      current-interceptor# (get-current-interceptor)]
     (set-current-interceptor (join-interceptors ~interceptors))
     (new-tracing-span (uuid) nil)
     (try
       ((get-current-interceptor) (fn [] (do ~@body)))
       (finally
         (set-tracing-context original-context#)
         (set-current-interceptor current-interceptor#)))))

(def ^:dynamic blah 1)
(binding
  [blah "x"]
  (println blah))

(defmacro traced-inherited
  [& body]
  `(let
     [original-context# (get-tracing-context)]
     (new-tracing-span (:trace-id original-context#) (:span-id original-context#))
     (try
       ((get-current-interceptor) (fn [] (do ~@body)))
       (finally
         (set-tracing-context original-context#)))))
