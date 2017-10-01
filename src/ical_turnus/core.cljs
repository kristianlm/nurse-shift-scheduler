(ns ical-turnus.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-time.core :as tm]
            [cljs-time.format :as fm]))

(enable-console-print!)

(defn ls-set!
  "Set `key' in browser's localStorage to `val`."
  [key val]
  (.setItem (.-localStorage js/window) key val))

(defn ls-get
  "Returns value of `key' from browser's localStorage."
  [key]
  (.getItem (.-localStorage js/window) key))

(defn ls-remove!
  "Remove the browser's localStorage value for the given `key`"
  [key]
  (.removeItem (.-localStorage js/window) key))

;; get all localStorage keys as a list of strings
(defn ls-keys []
  (map
    (fn [key] (.key (.-localStorage js/window) key))
    (range (.-length (.-localStorage js/window)))))

;; get all localStorage items as map
(defn ls-items []
  (into {} (map (fn [k] [k (ls-get k)]) (ls-keys))))


;; define your app data so that it doesn't get over-written on reload

(defonce app-state
  (atom {:scrollsize 3
         :turnus (ls-items)}))


;; TODO: make this faster by seeing which key have changed between o
;; and n.
(add-watch app-state :local-storage
  (fn [k r o n]
    (doseq [[k v] (:turnus @app-state)] (ls-set! k v))))


(defn day-after [date]
  (tm/plus date (tm/days 1)))

(defn days-since [n & [date]]
  ;; limit to 1000 just in case somebody (ehm) tries to print this
  ;; infinite sequence.
  (take n (iterate day-after (or date (tm/now)))))


(fm/unparse (fm/formatter "w") (tm/date-time 2017 01 02))

;; date => string
(defn d2s [date & [fmt]]
  (fm/unparse (fm/formatter (or fmt "YYYY-MM-dd")) date))

(defn choice [d]
  (let [lbl (fn ;; getter/setter for d
              ([] (get-in @app-state [:turnus d]))
              ([l] (swap! app-state update-in [:turnus d]
                     (fn [old]
                       (println d " now " l)
                       l))))
        btn
        (fn [label]
          [:button {:on-click (fn [] (lbl label))
                    :style {:background-color (if (= label (lbl))
                                                "#888"
                                                "#eee")}} label])]
    [:div
     (btn "D")
     (btn "A")
     (btn "N")
     (btn "F")
     (btn "?")]))

(defn hello-world []
  [:div
   [:h1 "Ingurnus"]

   [:div
    [:table
     [:tbody
      (doall
        (map
          (fn [x]
            (let [df (d2s x "YYYY-MM-dd")]
              [:tr {:key df}
               [:td (d2s x "w")]
               [:td (d2s x "E")]
               [:td (d2s x "YYYY-MM-dd")]
               [:td (choice (d2s x))]]))
          (days-since (:scrollsize @app-state))))]]]

   [:button {:on-click (fn [] (swap! app-state update-in [:scrollsize]
                                (fn [ss] (+ ss 100))))}
    "load more"]])

;; (hello-world)

(reagent/render-component [hello-world]
  (. js/document (getElementById "app")))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
