(import chicken.io 
        chicken.string
        dt
        srfi-13)

(define (two n) ;; how to do printf "%02d"?
  (let ((n (conc n)))
    (conc (if (= 1 (string-length n)) "0" "") n)))

(define (week->date year week day)
  (unless (number? year) (error "invalid year: " year))
  (receive (y m d) (d->ymd (ywd->d year week day))
    (conc y (two m) (two d))))

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
            (let ((date (week->date 2020 week day)))
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
;; (import fmt)
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

