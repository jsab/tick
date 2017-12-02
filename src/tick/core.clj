;; Copyright © 2016-2017, JUXT LTD.

(ns tick.core
  (:refer-clojure :exclude [+ - / inc dec max min range time int long < <= > >=])
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str])
  (:import
   [java.util Date]
   [java.time Clock ZoneId ZoneOffset Instant Duration Period DayOfWeek Month ZonedDateTime LocalTime LocalDateTime LocalDate Year YearMonth ZoneId OffsetDateTime]
   [java.time.format DateTimeFormatter]
   [java.time.temporal ChronoUnit]))

(def units
  {:nanos ChronoUnit/NANOS
   :micros ChronoUnit/MICROS
   :millis ChronoUnit/MILLIS
   :seconds ChronoUnit/SECONDS
   :minutes ChronoUnit/MINUTES
   :hours ChronoUnit/HOURS
   :half-days ChronoUnit/HALF_DAYS
   :days ChronoUnit/DAYS
   :weeks ChronoUnit/WEEKS
   :months ChronoUnit/MONTHS
   :years ChronoUnit/YEARS
   :decades ChronoUnit/DECADES
   :centuries ChronoUnit/CENTURIES
   :millennia ChronoUnit/MILLENNIA
   :eras ChronoUnit/ERAS
   :forever ChronoUnit/FOREVER})

(def ^{:dynamic true} *clock* nil)

(defn now []
  (if *clock*
    (Instant/now *clock*)
    (Instant/now)))

(defn just-now []
  (.truncatedTo (now) (ChronoUnit/SECONDS)))

(defn today []
  (if *clock*
    (LocalDate/now *clock*)
    (LocalDate/now)))

(defn epoch []
  (java.time.Instant/EPOCH))

(s/def ::instant #(instance? Instant %))

(defn parse-day [input]
  (condp re-matches (str/lower-case input)
    #"(mon)(day)?" DayOfWeek/MONDAY
    #"(tue)(s|sday)?" DayOfWeek/TUESDAY
    #"(wed)(s|nesday)?" DayOfWeek/WEDNESDAY
    #"(thur)(s|sday)?" DayOfWeek/THURSDAY
    #"(fri)(day)?" DayOfWeek/FRIDAY
    #"(sat)(urday)?" DayOfWeek/SATURDAY
    #"(sun)(day)?" DayOfWeek/SUNDAY
    nil))

(defn parse-month [input]
  (condp re-matches (str/lower-case input)
    #"(jan)(uary)?" Month/JANUARY
    #"(feb)(ruary)?" Month/FEBRUARY
    #"(mar)(ch)?" Month/MARCH
    #"(apr)(il)?" Month/APRIL
    #"may" Month/MAY
    #"(jun)(e)?" Month/JUNE
    #"(jul)(y)?" Month/JULY
    #"(aug)(ust)?" Month/AUGUST
    #"(sep)(tember)?" Month/SEPTEMBER
    #"(oct)(tober)?" Month/OCTOBER
    #"(nov)(ember)?" Month/NOVEMBER
    #"(dec)(ember)?" Month/DECEMBER
    nil))

(defprotocol IParseable
  (parse [_] "Parse to most applicable instance."))

(extend-protocol IParseable
  String
  (parse [s]
    (condp re-matches s
      #"(\d{1,2})\s*(am|pm)"
      :>> (fn [[_ h ap]] (LocalTime/of (cond-> (Integer/parseInt h) (= "pm" ap) (clojure.core/+ 12)) 0))
      #"(\d{1,2})"
      :>> (fn [[_ h]] (LocalTime/of (Integer/parseInt h) 0))
      #"\d{2}:\d{2}\S*"
      :>> (fn [s] (LocalTime/parse s))
      #"(\d{1,2}):(\d{2})"
      :>> (fn [[_ h m]] (LocalTime/of (Integer/parseInt h) (Integer/parseInt m)))
      #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z"
      :>> (fn [s] (Instant/parse s))
      #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?[+-]\d{2}:\d{2}"
      :>> (fn [s] (OffsetDateTime/parse s))
      #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?[+-]\d{2}:\d{2}\[\w+/\w+\]"
      :>> (fn [s] (ZonedDateTime/parse s))
      #"\d{4}-\d{2}-\d{2}T\S*"
      :>> (fn [s] (LocalDateTime/parse s))
      #"\d{4}-\d{2}-\d{2}"
      :>> (fn [s] (LocalDate/parse s))
      #"\d{4}-\d{2}"
      :>> (fn [s] (YearMonth/parse s))
      #"\d{4}"
      :>> (fn [s] (Year/parse s))
      (throw (ex-info "Unparseable time string" {:input s})))))

(defprotocol IConstructors
  (date [_] "Make a java.time.LocalDate instance.")
  (day [_] "Make a java.time.DayOfWeek instance.")
  (day-of-month [_] "Return value of the day in the month as an integer.")
  (inst [_] "Make a java.util.Date instance.")
  (instant [_] "Make a java.time.Instant instance.")
  (int [_] "Return value as integer")
  (long [_] "Return value as long")
  (month [_] "Make a java.time.Month instance.")
  (offset-date-time [_] "Make a java.time.OffsetDateTime instance.")
  (year [_] "Make a java.time.Year instance.")
  (year-month [_] "Make a java.time.YearMonth instance.")
  (zone [_] "Make a java.time.ZoneId instance.")
  (zoned-date-time [_] "Make a java.time.ZonedDateTime instance."))

(extend-protocol IConstructors
  Object
  (int [v] (clojure.core/int v))
  (long [v] (clojure.core/long v))

  clojure.lang.Fn
  (date [f] (date (f)))
  (day [f] (day (f)))
  (inst [f] (inst (f)))
  (instant [f] (instant (f)))
  (int [f] (int (f)))
  (long [f] (long (f)))
  (month [f] (month (f)))
  (offset-date-time [f] (offset-date-time (f)))
  (year [f] (year (f)))
  (year-month [f] (year-month (f)))
  (zone [f] (zone (f)))
  (zoned-date-time [f] (zone (f)))

  Instant
  (inst [i] (Date/from i))
  (instant [i] i)
  (date [i] (date (zoned-date-time i)))
  (day [i] (day (date i)))
  (month [i] (month (date i)))
  (year [i] (year (date i)))
  (year-month [i] (year-month (date i)))
  (zoned-date-time [i] (.atZone i ZoneOffset/UTC))
  (int [i] (.getNano i))
  (long [i] (.getEpochSecond i))

  String
  (inst [s] (inst (instant s)))
  (instant [s] (instant (parse s)))
  (day [s] (or (parse-day s) (day (date s))))
  (date [s] (date (parse s)))
  (month [s] (parse-month s))
  (year [s] (year (parse s)))
  (year-month [s] (year-month (parse s)))
  (zone [s] (ZoneId/of s))
  (int [s] (.getNano (instant s)))
  (long [s] (.getEpochSecond (instant s)))

  Number
  (day [n] (DayOfWeek/of n))
  (month [n] (Month/of n))
  (instant [n] (Instant/ofEpochSecond n))
  (year [n] (Year/of n))

  LocalDate
  (date [d] d)
  (day [d] (.getDayOfWeek d))
  (day-of-month [d] (.getDayOfMonth d))
  (month [d] (Month/from d))
  (year-month [d] (YearMonth/of (.getYear d) (.getMonthValue d)))
  (year [d] (Year/of (.getYear d)))

  Month
  (int [m] (.getValue m))

  LocalDateTime
  (date [dt] (.toLocalDate dt))
  (day [dt] (day (date dt)))
  (day-of-month [dt] (day-of-month (date dt)))
  (year-month [dt] (year-month (date dt)))
  (year [dt] (year (date dt)))

  Date
  (inst [d] d)
  (instant [d] (.toInstant d))
  (date [d] (date (zoned-date-time (instant d)))) ; implicit conversion to UTC
  (year-month [d] (year-month (date d)))
  (year [d] (year (date d)))

  YearMonth
  (year-month [ym] ym)
  (year [ym] (year (.getYear ym)))

  Year
  (year [y] y)
  (int [y] (.getValue y))

  ZoneId
  (zone [z] z)

  ZonedDateTime
  (inst [zdt] (inst (instant zdt)))
  (instant [zdt] (.toInstant zdt))
  (date [zdt] (.toLocalDate zdt))
  (zone [zdt] (.getZone zdt)))

(defprotocol IDuration
  (nanos [_] "Return the given quantity in nanoseconds.")
  (millis [_] "Return the given quantity in milliseconds.")
  (seconds [_] "Return the given quantity in seconds.")
  (minutes [_] "Return the given quantity in minutes.")
  (hours [_] "Return the given quantity in hours.")
  (days [_] "Return the given quantity in days."))

(extend-protocol IDuration
  Number
  (nanos [n] (Duration/ofNanos n))
  (millis [n] (Duration/ofMillis n))
  (seconds [n] (Duration/ofSeconds n))
  (minutes [n] (Duration/ofMinutes n))
  (hours [n] (Duration/ofHours n))
  (days [n] (Duration/ofDays n))

  Duration
  (nanos [d] (.toNanos d))
  (millis [d] (.toMillis d))
  (seconds [d] (.getSeconds d))
  (minutes [d] (.toMinutes d))
  (hours [d] (.toHours d))
  (days [d] (.toDays d)))

(defprotocol IDurationCoercion
  (duration [_] [_ _] "Return the duration of the given value, or construct a duration between an amount and units"))

(extend-protocol IDurationCoercion
  Duration
  (duration [d] d)
  nil
  (duration [_] nil)
  Number
  (duration
    ([n] (duration n :seconds))
    ([n u] (let [unit (units u)]
             (assert unit (str "Not a unit: " u))
             (Duration/of n unit)))))

(defprotocol IPeriodCoercion
  (period [_ _] "Construct a period of an amount and units"))

(extend-protocol IPeriodCoercion
  nil
  (period [_ _] nil)
  Number
  (period
    [n u] (case u
            :days (Period/ofDays n)
            :weeks (Period/ofWeeks n)
            :months (Period/ofMonths n)
            :years (Period/ofYears n))))

(defn between [i1 i2]
  (Duration/between (instant i1) (instant i2)))

(defprotocol ITimeComparison
  (< [x y] "Is x before y?")
  (<= [x y] "Is x before or at the same time as y?")
  (> [x y] "Is x after y?")
  (>= [x y] "Is x after or at the same time as y?"))

(extend-protocol ITimeComparison
  Instant
  (< [x y] (.isBefore x y))
  (<= [x y] (not (.isAfter x y)))
  (> [x y] (.isAfter x y))
  (>= [x y] (not (.isBefore x y)))
  LocalDateTime
  (< [x y] (.isBefore x y))
  (<= [x y] (not (.isAfter x y)))
  (> [x y] (.isAfter x y))
  (>= [x y] (not (.isBefore x y)))
  OffsetDateTime
  (< [x y] (.isBefore x y))
  (<= [x y] (not (.isAfter x y)))
  (> [x y] (.isAfter x y))
  (>= [x y] (not (.isBefore x y)))
  ZonedDateTime
  (< [x y] (.isBefore x y))
  (<= [x y] (not (.isAfter x y)))
  (> [x y] (.isAfter x y))
  (>= [x y] (not (.isBefore x y))))

(defprotocol ITimeArithmetic
  (+ [_ _] "Add time")
  (- [_ _] "Subtract time")
  (inc [_] "Increment time")
  (dec [_] "Decrement time")
  (max [_ _] "Return maximum")
  (min [_ _] "Return minimum")
  (range [_] [_ _] [_ _ _] "Returns a lazy seq of times from start (inclusive) to end (exclusive, nil means forever), by step, where start defaults to 0, step to 1, and end to infinity."))

(defprotocol IDivisible
  (/ [_ _] "Divide time"))

(extend-protocol IDivisible
  String
  (/ [s d] (/ (parse s) d)))

(extend-type Instant
  ITimeArithmetic
  (+ [t x] (.plus t x))
  (- [t x] (.minus t x))
  (inc [t] (+ t (seconds 1)))
  (dec [t] (- t (seconds 1)))
  (max [x y] (if (neg? (compare x y)) y x))
  (min [x y] (if (neg? (compare x y)) x y))
  (range
    ([from] (iterate #(.plusSeconds % 1) from))
    ([from to] (cond->> (iterate #(.plusSeconds % 1) from)
                 to (take-while #(< % to))))
    ([from to step] (cond->> (iterate #(.plus % step) from)
                      to (take-while #(< % to))))))

(extend-type ZonedDateTime
  ITimeArithmetic
  (+ [t x] (.plus t x))
  (- [t x] (.minus t x))
  (inc [t] (+ t (seconds 1)))
  (dec [t] (- t (seconds 1)))
  (max [x y] (if (neg? (compare x y)) y x))
  (min [x y] (if (neg? (compare x y)) x y))
  (range
    ([from] (iterate #(.plusSeconds % 1) from))
    ([from to] (cond->> (iterate #(.plusSeconds % 1) from)
                 to (take-while #(< % to))))
    ([from to step] (cond->> (iterate #(.plus % step) from)
                      to (take-while #(< % to))))))

(extend-type LocalDate
  ITimeArithmetic
  (+ [t x] (if (number? x) (.plusDays t x) (.plus t x)))
  (- [t x] (if (number? x) (.minusDays t x) (.minus t x)))
  (inc [t] (.plusDays t 1))
  (dec [t] (.minusDays t 1))
  (max [x y] (if (neg? (compare x y)) y x))
  (min [x y] (if (neg? (compare x y)) x y))
  (range
    ([from] (iterate #(.plusDays % 1) from))
    ([from to] (cond->> (iterate #(.plusDays % 1) from)
                 to (take-while #(< % to) )))
    ([from to step] (cond->> (iterate #(.plusDays % step) from)
                      to (take-while #(< % to))))))

(extend-type LocalDateTime
  ITimeArithmetic
  (+ [t x] (.plus t x))
  (- [t x] (.minus t x))
  (inc [t] (+ t (seconds 1)))
  (dec [t] (- t (seconds 1)))
  (max [x y] (if (neg? (compare x y)) y x))
  (min [x y] (if (neg? (compare x y)) x y))
  (range
    ([from] (iterate #(.plusSeconds % 1) from))
    ([from to] (cond->> (iterate #(.plusSeconds % 1) from)
                 to (take-while #(< % to) )))
    ([from to step] (cond->> (iterate #(.plus % step) from)
                      to (take-while #(< % to))))))

(extend-type YearMonth
  ITimeArithmetic
  (+ [t x] (if (number? x) (.plusMonths t x) (.plus t x)))
  (- [t x] (if (number? x) (.minusMonths t x) (.minus t x)))
  (inc [t] (.plusMonths t 1))
  (dec [t] (.minusMonths t 1))
  (max [x y] (if (neg? (compare x y)) y x))
  (min [x y] (if (neg? (compare x y)) x y))
  (range
    ([from] (iterate #(.plusMonths % 1) from))
    ([from to] (cond->> (iterate #(.plusMonths % 1) from)
                 to (take-while #(< % to) )))
    ([from to step] (cond->> (iterate #(.plus % step) from)
                      to (take-while #(< % to))))))

(extend-type Year
  ITimeArithmetic
  (+ [t x] (if (number? x) (.plusYears t x) (.plus t x)))
  (- [t x] (if (number? x) (.minusYears t x) (.minus t x)))
  (inc [t] (.plusYears t 1))
  (dec [t] (.minusYears t 1))
  (max [x y] (if (neg? (compare x y)) y x))
  (min [x y] (if (neg? (compare x y)) x y))
  (range
    ([from] (iterate #(.plusYears % 1) from))
    ([from to] (cond->> (iterate #(.plusYears % 1) from)
                 to (take-while #(< % to) )))
    ([from to step] (cond->> (iterate #(.plus % step) from)
                      to (take-while #(< % to))))))

(defprotocol IDivisbleDuration
  (divide-duration [divisor duration] "Divide a duration"))

(extend-protocol IDivisbleDuration
  Long
  (divide-duration [n duration] (.dividedBy duration n))
  Duration
  (divide-duration [divisor duration]
    (clojure.core// (.getSeconds duration) (.getSeconds divisor))))

(extend-type Duration
  ITimeArithmetic
  (+ [d x] (.plus d x))
  (- [d x] (.minus d x))
  (inc [d] (.plusSeconds d 1))
  (dec [d] (.minusSeconds d 1))
  (max [x y] (if (neg? (compare x y)) y x))
  (min [x y] (if (neg? (compare x y)) x y))
  IDivisible
  (/ [d x] (divide-duration x d)))

(defn tomorrow []
  (+ (today) 1))

(defn yesterday []
  (- (today) 1))

(defprotocol ITime
  (time [s] "Constructor of an instant, inst, java.time.LocalTime or java.time.LocalDateTime?")
  (local? [t] "Is the time a java.time.LocalTime or java.time.LocalDateTime?"))

(defprotocol ITimeRange
  (start [_] "Return the start of a time period.")
  (end [_] "Return the end of a time period."))

(extend-protocol IDurationCoercion
  Object
  (duration [v] (Duration/between (start v) (end v))))

(extend-protocol ITime
  String
  (time [s] (time (parse s)))

  Number
  (time [i]
    (LocalTime/of i 0))

  Date
  (time [d] (instant d))
  (local? [d] false)

  Instant
  (time [i] i)
  (local? [i] false)

  LocalDateTime
  (time [i] i)
  (local? [i] true)

  LocalTime
  (time [i] i)
  (local? [i] true)

  nil
  (time [_] nil)
  (local? [_] nil))

(extend-protocol ITimeRange
  String
  (start [s] (start (time s)))
  (end [s] (end (time s)))

  Number
  (start [n] (start (time n)))
  (end [n] (end (time n)))

  LocalDate
  (start [date] (.atStartOfDay date))
  (end [date] (.atStartOfDay (inc date)))

  Year
  (start [year] (start (.atMonth year 1)))
  (end [year] (end (.atMonth year 12)))

  YearMonth
  (start [ym] (start (.atDay ym 1)))
  (end [ym] (end (.atEndOfMonth ym)))

  Instant
  (start [i] i)
  (end [i] i)

  Date
  (start [i] (instant i))
  (end [i] (instant i))

  LocalDateTime
  (start [time] time)
  (end [end] end)

  nil
  (start [_] nil)
  (end [_] nil))

(defn on [^LocalTime time ^LocalDate date]
  (.atTime date time))

(defn at [^LocalDate date ^LocalTime time]
  (.atTime date time))

(defn midnight [^LocalDate date]
  (at date (LocalTime/MIDNIGHT)))

(defn noon [^LocalDate date]
  (at date (LocalTime/NOON)))

(defn midnight? [^LocalDateTime t]
  (.isZero (Duration/between t (start (date t)))))

(defprotocol IAtZone
  (at-zone [t zone] "Put time at zone")
  (to-local [t] [t zone] "Convert to local time at zone."))

(extend-protocol IAtZone
  LocalDateTime
  (at-zone [t zone] (.atZone t zone))
  (to-local
    ([t] t)
    ([t zone] (to-local (at-zone t zone))))
  Instant
  (at-zone [t zone] (.atZone t zone))
  (to-local
    ([t] (throw (ex-info "Error, zone required" {})))
    ([t zone] (to-local (at-zone t zone))))
  ZonedDateTime
  (at-zone [t zone] (.withZoneSameInstant t zone))
  (to-local
    ([t] (.toLocalDateTime t))
    ([t zone] (to-local (at-zone t zone))))
  Date
  (at-zone [t zone] (at-zone (instant t) zone))
  (to-local
    ([t] (throw (ex-info "Error, zone required" {})))
    ([t zone] (to-local (at-zone t zone)))))

(defprotocol MinMax
  (min-of-type [_] "Return the min")
  (max-of-type [_] "Return the max"))

(extend-protocol MinMax
  LocalDateTime
  (min-of-type [_] (LocalDateTime/MIN))
  (max-of-type [_] (LocalDateTime/MAX))
  Instant
  (min-of-type [_] (Instant/MIN))
  (max-of-type [_] (Instant/MAX))
  ZonedDateTime
  (min-of-type [_] (Instant/MIN))
  (max-of-type [_] (Instant/MAX))
  ;; TODO: This may cause surprises - see clojure/java-time. We should
  ;; change the semantics of nil to not imply epoch, forever, or
  ;; whatever.
  nil
  (min-of-type [_] (Instant/MIN))
  (max-of-type [_] (Instant/MAX)))


;; first/last using java.time.temporal/TemporalAdjuster
;; See also java.time.temporal/TemporalAdjusters

;; java.time.temporal/TemporalAmount

#_(defn adjust [t adjuster]
  (.with t adjuster))

;; adjust


;; Conversions
