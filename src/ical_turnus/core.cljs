(ns ical-turnus.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-time.core :as tm]
            [cljs-time.format :as fm]
            [cljs-time.extend] ;; make = work
            [ical-turnus.localstorage :as ls]))

(enable-console-print!)


;; DATE => string
(defn d2s [date & [fmt]]
  (fm/unparse (fm/formatter (or fmt "YYYY-MM-dd")) date))

;; define your app data so that it doesn't get over-written on reload

(defonce app-state
  (atom {:scrollsize 3
         :turnus
         (into {}
           (map (fn [[key label]]
                  [(fm/parse (fm/formatter "YYYY-MM-dd") key) label])
             (ls/items)))
         :cursor (tm/date-time 2017 01 11)}))

;; TODO: make this faster by seeing which key have changed between o
;; and n.
(add-watch app-state :local-storage
  (fn [k r o n]
    (doseq [[k v] (:turnus @app-state)]
      (ls/set! (d2s k) v))))


;; creates a temporary link with uri-encoded file content. allows
;; download without resorting to a server or real file.
;;
;; see https://stackoverflow.com/questions/3665115/create-a-file-in-memory-for-user-to-download-not-through-server
(defn download! [filename content mime]
  (let [el (doto (.createElement js/document "a")
             (.setAttribute "href" (str "data:" mime ";charset=utf-8,"
                                     (js/encodeURIComponent content)))
             (.setAttribute "download" filename))]
    (set! (.-display (.-style el)) "none")
    (set! (.-innerHtml el) "foool")
    (.appendChild (.-body js/document) el)
    (.click el)
    (.removeChild (.-body js/document) el)))

(defn day-after [date]
  (tm/plus date (tm/days 1)))

(defn days-since [n & [date]]
  ;; limit to 1000 just in case somebody (ehm) tries to print this
  ;; infinite sequence.
  (take n (iterate day-after (or date (tm/now)))))

;; Day of Week (beacuse "e" format gives error)
;; (dow (tm/date-time 2017 01 04)) => 3 (Wed)
;; 1 is monday, 7 is sunday.
(defn dow [d] ;; using cljs-time uses Google Closure Library's
  (let [dowsun (.getDay d)]
    (if (= dowsun 0) 6 (- dowsun 1))))


;; tested like this:
;; (map (fn [w] (map (fn [d] [d (dow (w2d 2018 w d))]) (range 1 8))) (range 0 52))
;; (w2d 2017 39 7) => Nov 1.
(defn w2d [[y ww day]] ;; day is zero-indexed
  (let [correction (+ (dow (tm/date-time y 01 04)) 3)
        ordinal (- (+ (* ww 7) day )
                  correction)]
    (tm/date-time y 1 ordinal)))

;; (w2d [2017 39 6])

(defn d2w [d]
  [(tm/year d)
   (tm/week-number-of-year d)
   (- (tm/day-of-week d) 1)])


(defn goto [d]
  (swap! app-state update-in [:cursor]
    (fn [old] (println "goto " d) d)))

(defn keyboard-period [event]
  (case (.-keyCode event)
    (37) (tm/days   -1)
    (38) (tm/weeks  -1)
    (39) (tm/days    1)
    (40) (tm/weeks   1)
    nil))

(defn keyboard-label [event]
  ;;(.log js/console (.-key event))
  (let [l (clojure.string/upper-case (.-key event))]
    (case l
      ("DELETE" " ") nil
      l)))


;; ;; our API is kinda silly.
;; (defn w+ [from [y w d]]
;;   (-> (tm/plus (w2d from)
;;         (tm/years y)
;;         (tm/weeks w)
;;         (tm/days d))
;;     (d2w)))

;; (move! [0 0 1]) ;; next day
(defn move-by! [period]
  (and period
    (swap! app-state update-in [:cursor]
      (fn [d] (tm/plus d period)))))

(defn mark! [cursor label]
  (swap! app-state update-in [:turnus cursor]
    (fn [old-label]
      (println "mark " cursor " = " label)
      label))
  (move-by! (tm/days 1)))

(defn render-day [d cursor? label]
  [:div
   { :class "daycell"
    :style {:color (if (= (dow d) 6) "#b00" "#000")
            :background-color
            (if cursor?
              "#f00"
              (if (zero? (mod (.getMonth d) 2))
                "#fff" "#ddd"))}
    :on-click #(goto d)}
   [:div {:class "date"}       (.getDate d)] " "
   [:div {:class "label"} label]])

;;(set! (.-testdate js/window) (tm/date-time 2017 1 1))


;; thanks to:
;; - https://theeventscalendar.com/support/forums/topic/ics-how-to-specify-all-day-event/
;; - https://en.wikipedia.org/wiki/ICalendar
;;
;; but not really https://tools.ietf.org/html/rfc5545 because it's way
;; too much to read.
(defn generate-turnus-ics [turnus]
  (clojure.string/join
    "\n"
    (concat
      ["BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//ingurnus
"]
      (for [[d label] turnus]
        (and label
          (let [s (d2s d "YYYYMMdd")]
            (str "BEGIN:VEVENT
UID:" s "@ingurnus
DTSTART;VALUE=DATE:" s "
SUMMARY:" label "
END:VEVENT
"))))
      ["END:VCALENDAR"])))

;;(println (generate-turnus-ics (:turnus @app-state)))

(defn render-calendar []
  [:table {:on-key-press (fn [event] (mark! (:cursor @app-state)
                                       (keyboard-label event)))
           :on-key-down (fn [event] (move-by! (keyboard-period event)))
           :tab-index 0}
   [:tbody
    (let [cursor (:cursor @app-state)
          turnus (:turnus @app-state)]
      (for [[yyyy w]
            (->> (range 1 (:scrollsize @app-state))
              (map (fn [w] [2017 w])))]
        [:tr {:key (str yyyy "W" w "-")
              :style {:margin 0}}
         [:td yyyy]
         [:td [:em {:style {:color "#66a"
                            :padding-right 10}}
               w]]
         [:td {:style {:font-size 13}}
          (fm/unparse (fm/formatter "MMM") (w2d [yyyy w 1]))]

         (for [day [0 1 2 3 4 5 6]]
           (let [d (w2d [yyyy w day])]
             [:td {:key day}
              (render-day d
                (= d cursor)
                (get turnus d))]))]))]])

(defn hello-world []
  [:div
   [:h1 "Ingurnus"]
   (render-calendar)
   [:button {:on-click (fn [] (swap! app-state update-in [:scrollsize]
                                (fn [ss] (+ ss 25))))}
    "load more"]
   [:div "Last ned "
    [:a {:href "#"
         :on-click
         (fn [e]
           (let [ics (generate-turnus-ics (:turnus @app-state))]
             (download! "one.ics" ics "text/calendar")))}
     "her"]]])


(reagent/render-component [hello-world]
  (. js/document (getElementById "app")))

