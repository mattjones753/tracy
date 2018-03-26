(ns tracy.jetty.jetty-extractor
  (:require
    [tracy.core :as core])
  (:import
    [javax.servlet.http HttpServletRequest]
    [org.eclipse.jetty.server.handler HandlerWrapper]))

(defn- trace-context-from-request
  [request header-names-and-defaults]
  (reduce-kv
    (fn [m key value]
      (assoc m key (or
                     (.getHeader request (name key))
                     (if (fn? value) (value) value))))
    {}
    header-names-and-defaults))

(defn tracing-handler-factory
  [interceptors trace-keys]
  (proxy [HandlerWrapper] []
    (handle [target baseRequest ^HttpServletRequest request response]
      (core/traced-with-context
        (trace-context-from-request request trace-keys)
        interceptors
        (proxy-super handle target baseRequest request response)))))
