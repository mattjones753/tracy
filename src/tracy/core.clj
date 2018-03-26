(ns tracy.core)

(defn- default-interceptor
  "The default, empty, interceptor function.  Just calls its argument."
  [main-function]
  (main-function))

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

(defn get-tracing-context
  []
  (merge {} (.get thread-local-context)))

(defn set-tracing-context
  [m]
  (.set thread-local-context m)
  thread-local-context)

(defmacro traced-with-context
  [request-context interceptors & body]
  `(let
     [original-context# (get-tracing-context)
      interceptor# (join-interceptors ~interceptors)]
     (set-tracing-context ~request-context)
     (try
       (interceptor# (fn [] (do ~@body)))
       (finally
         (set-tracing-context original-context#)))))

