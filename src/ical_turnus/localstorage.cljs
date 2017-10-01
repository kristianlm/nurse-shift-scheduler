(ns ical-turnus.localstorage
  (:refer-clojure :exclude [get set keys]))

(defn set!
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn get
  "Returns value of `key' from browser's localStorage."
  [key]
  (.getItem (.-localStorage js/window) key))

(defn remove!
  "Remove the browser's localStorage value for the given `key`"
  [key]
  (.removeItem (.-localStorage js/window) key))

;; get all localStorage keys as a list of strings
(defn keys []
  (map
    (fn [key] (.key (.-localStorage js/window) key))
    (range (.-length (.-localStorage js/window)))))

;; get all localStorage items as map
(defn items []
  (into {} (map (fn [k] [k (get k)]) (keys))))


