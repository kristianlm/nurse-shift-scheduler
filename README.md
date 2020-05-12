
## shift schedule

Turn a file that looks like this:

```scheme
;; 2020
;;
(;;  MTWTFSS
 ( 3 dafnnff)
 ( 4 ddufaad)
 ( 5 dfaadff)
 ( 6 faadfff)
 ( 7 fadfaad) )
```

Into a file that looks like this:

```
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//ingurnus

BEGIN:VEVENT
UID:20200103@nurse-shift-schedule
DTSTART;VALUE=DATE:20200103
SUMMARY:d                            # week 1, monday
END:VEVENT

BEGIN:VEVENT
UID:20200104@nurse-shift-schedule
DTSTART;VALUE=DATE:20200104
SUMMARY:a                            # week 1, tuesday
END:VEVENT

... long list of events
```
