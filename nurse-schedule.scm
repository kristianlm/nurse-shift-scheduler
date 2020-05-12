(import chicken.io 
        chicken.time.posix
        chicken.string
        srfi-13 fmt)

(define (week->date
         day  ;; 0 is sunday, 1 is monday etc
         week ;; %W 2 is %V 1, and %V doesn't work
         #!optional format 
         year)
  (time->string
   (string->time
    (conc "d" day "y"
          (or year (string->number (time->string (seconds->local-time) "%Y")))
          "w" (- week 1))
    "d%wy%Yw%W")
   (or format "%y.%m.%d")))

(define cal (read))
(begin
  (print "BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//ingurnus
")
  (for-each
   (lambda (row) ;; row is (3 dddaaan)
     (let ((week (car row))
           (shifts (string->list (symbol->string (cadr row)))))
       (unless (= 7 (length shifts)) (error "invalid week shift spec" row))
       (for-each
        (lambda (day shift)
          (unless (eq? shift #\F)
            (let ((date (week->date day week "%Y%m%d")))
              (print "
BEGIN:VEVENT
UID:" date "@ingurnus
DTSTART;VALUE=DATE:" date #|20171023|# "
SUMMARY:" shift "
END:VEVENT"))))
        '(1 2 3 4 5 6 0)
        shifts)))
   cal)
  (print "
END:VCALENDAR"))


;;; alternative tabular output
;; (begin
;;   (fmt #t nl (fit 23) (fmt-join (lambda (x) (fit/left 4 (upcase x)))
;;                                 `(m t o t f l s)) nl)
;;   (for-each
;;    (lambda (row)
;;      (let ((week (car row))
;;            (days (string->list (symbol->string (cadr row)))))
;;       
;;        (fmt #t
;;             (fit 23 "k:" (fit/left 2 week) " "
;;                  (week->date 1 week) "-"
;;                  (week->date 0 week) " ")
;;             (fmt-join (cut fit/left 4 <>) days)
;;             nl)))
;;    cal))

