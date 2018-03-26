(ns tracy.halboy.halboy
  (:require
    [tracy.core :as core]
    [halboy.navigator :as navigator]
    [clojure.walk :as walk]))

; navigator stuff
(defn- get-headers
  []
  {:http {:headers (walk/stringify-keys (core/get-tracing-context))}})

(defn resume
  ([resource]
   (resume resource {}))
  ([resource options]
   (navigator/resume resource (merge (get-headers) options))))

(defn discover
  ([resource]
   (discover resource {}))
  ([resource options]
   (navigator/discover resource (merge (get-headers) options))))
