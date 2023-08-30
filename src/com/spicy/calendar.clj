(ns com.spicy.calendar
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.results.ui :as r]
    [java-time.api :as jt]
    [java-time.repl :as jtr]))


(defn time-frame-start-day
  [local-date-now]
  (let [first-sunday-of-the-month (jt/adjust local-date-now :first-in-month :sunday)
        first-sunday-day          (jt/as first-sunday-of-the-month :day-of-month)
        first-is-sunday?          (and (= 1 first-sunday-day) (jt/sunday? first-sunday-of-the-month))
        last-month                (jt/minus local-date-now (jt/months 1))
        last-sunday-of-last-month (jt/adjust last-month :last-in-month :sunday)]
    (if first-is-sunday?
      first-sunday-of-the-month
      last-sunday-of-last-month)))


(defn monthly-frame-days
  ([local-date-now]
   (monthly-frame-days local-date-now 42))
  ([local-date-now days-to-take]
   (take days-to-take (jt/iterate jt/plus (time-frame-start-day local-date-now) (jt/days 1)))))


(comment

  (monthly-frame-days (jt/local-date))
  (jt/iterate jt/plus (jt/local-date) (jt/days 1))

  (jt/local-date-time 2023 8)

  (jt/range (jt/period 2023 8))
  (jt/standard-days 31)

  (jt/minus (jt/local-date 2023 10 31) (jt/months 1))
  (jt/adjust (jt/year-month) :first-in-month :sunday)

  (jt/as (jt/adjust (jt/local-date 2023 8 1) :first-in-month :sunday) :day-of-month)

  (map #(jt/local-date 2023 8 %) (map inc (range (.lengthOfMonth (jt/year-month)))))

  (jt/adjust (jt/local-date-time) :last-in-month :friday))


(defn container
  [& children]
  [:div.lg:flex.lg:h-full.lg:flex-col
   children])


(defn header
  [{:keys [date today]}]
  [:header.flex.items-center.justify-between.border-b.border-gray-200.px-6.py-4.lg:flex-none
   {:id "calendar-header"}
   [:h1.text-base.font-semibold.leading-6.text-gray-900
    [:time {:datetime (jt/format "YYYY-MM" date)} (jt/format "MMMM YYYY" date)]]
   [:div.flex.items-center
    [:div.relative.flex.items-center.rounded-md.bg-white.shadow-sm.md:items-stretch
     [:button.flex.h-9.w-12.items-center.justify-center.rounded-l-md.border-y.border-l.border-gray-300.pr-1.text-gray-400.hover:text-gray-500.focus:relative.md:w-9.md:pr-0.md:hover:bg-gray-50
      {:type "button"
       :hx-get (str "/app/results/calendar?date=" (jt/format "YYYY-MM-dd" (jt/minus date (jt/months 1))))
       :hx-select "#calendar-header"
       :hx-target "#calendar-header"
       :hx-select-oob "#calendar"
       :hx-swap "outerHTML"}
      [:span.sr-only "Previous month"]
      [:svg.h-5.w-5 {:viewBox "0 0 20 20" :fill "currentcolor" :aria-hidden "true"}
       [:path {:fill-rule "evenodd" :d "M12.79 5.23a.75.75 0 01-.02 1.06L8.832 10l3.938 3.71a.75.75 0 11-1.04 1.08l-4.5-4.25a.75.75 0 010-1.08l4.5-4.25a.75.75 0 011.06.02z" :clip-rule "evenodd"}]]]
     [:button.hidden.border-y.border-gray-300.px-3.5.text-sm.font-semibold.text-gray-900.hover:bg-gray-50.focus:relative.md:block
      {:type "button"
       :hx-get (str "/app/results/calendar?date=" (jt/format "YYYY-MM-dd" today))
       :hx-select "#calendar-header"
       :hx-target "#calendar-header"
       :hx-select-oob "#calendar"
       :hx-swap "outerHTML"}
      "Today"]
     [:span.relative.-mx-px.h-5.w-px.bg-gray-300.md:hidden]
     [:button.flex.h-9.w-12.items-center.justify-center.rounded-r-md.border-y.border-r.border-gray-300.pl-1.text-gray-400.hover:text-gray-500.focus:relative.md:w-9.md:pl-0.md:hover:bg-gray-50
      {:type "button"
       :hx-get (str "/app/results/calendar?date=" (jt/format "YYYY-MM-dd" (jt/plus date (jt/months 1))))
       :hx-select "#calendar-header"
       :hx-target "#calendar-header"
       :hx-select-oob "#calendar"
       :hx-swap "outerHTML"}
      [:span.sr-only "Next month"]
      [:svg.h-5.w-5 {:viewBox "0 0 20 20" :fill "currentcolor" :aria-hidden "true"}
       [:path {:fill-rule "evenodd" :d "M7.21 14.77a.75.75 0 01.02-1.06L11.168 10 7.23 6.29a.75.75 0 111.04-1.08l4.5 4.25a.75.75 0 010 1.08l-4.5 4.25a.75.75 0 01-1.06-.02z" :clip-rule "evenodd"}]]]]]])


(defn inst->month-day
  [i]
  (-> i
      (jt/local-date (jt/zone-id))
      (jt/as :month-of-year :day-of-month)))


(defn desktop-day
  [{:keys [day date now results] :as _day-props}]
  (let [is-today? (= (jt/as day :year :month-of-year :day-of-month)
                     (jt/as now :year :month-of-year :day-of-month))
        is-current-month? (= (jt/as day :month-of-year) (jt/as date :month-of-year))]
    [:div {:class (concat '[relative px-3 py-2]
                          (when-not is-current-month? '[bg-gray-50 text-gray-500])
                          (when is-current-month? '[bg-white]))}
     [:time {:class (concat [] (when is-today? '[flex h-6 w-6 items-center justify-center rounded-full bg-brand-teal font-semibold]))
             :datetime (jt/format "YYYY-MM-dd" day)} (jt/as day :day-of-month)]
     (when (not-empty results)
       [:ol.mt-2.list-none.m-0.p-0
        (map (comp (fn [{:keys [name href]}]
                     [:li
                      [:a.group.flex {:href href}
                       [:p.flex-auto.truncate.font-medium.font-semibold.text-gray-900.group-hover:text-brand-teal name]]]) r/normalized-result) results)])]))


(defn mobile-day
  [{:keys [day date now results] :as _day-props}]
  (let [is-today? (= (jt/as day :year :month-of-year :day-of-month)
                     (jt/as now :year :month-of-year :day-of-month))
        is-current-month? (= (jt/as day :month-of-year) (jt/as date :month-of-year))]
    [:button
     {:type "button"
      :hx-get (str "/app/results/calendar-day?date=" (jt/format "YYYY-MM-dd" day))
      :hx-swap "outerHTML"
      :hx-target "#mobile-day-view"
      :class (concat '[flex h-14 flex-col px-3 py-2 text-gray-900 hover:bg-gray-100 focus:z-10]
                     (when-not is-current-month? '[bg-gray-50 text-gray-500])
                     (when is-current-month? '[bg-white])
                     (when is-today? '[font-semibold]))}
     [:time.ml-auto
      {:datetime (jt/format "YYYY-MM-dd" day)
       :class (concat '[ml-auto]
                      (when is-today? '[flex h-6 w-6 items-center justify-center rounded-full bg-brand-teal font-semibold]))}
      (jt/as day :day-of-month)]
     [:span.sr-only (str (count results) " events")]
     (when (not-empty results)
       [:span.-mx-0.5.mt-auto.flex.flex-wrap-reverse
        (map (fn [_] [:span.mx-0.5.mb-1.h-1.5.w-1.5.rounded-full.bg-gray-400]) results)])]))


(defmulti ->instant
  (fn [x] (type x)))


(defmethod ->instant java.time.LocalDate
  [local-date]
  (.toInstant (.atStartOfDay local-date (jt/zone-id))))


(defmethod ->instant java.time.ZonedDateTime
  [zdt]
  (.toInstant zdt))


(defmethod ->instant java.time.Instant
  [instant]
  instant)


(defn ->date
  [d]
  (jt/java-date (->instant d)))


(defn safe-parse-date
  [s]
  (try
    (map #(Integer/parseInt %) (string/split s #"-"))
    (catch Exception _e
      '())))


(defn day-view
  [{:keys [biff/db params session]}]
  (let [date (apply jt/local-date (safe-parse-date (:date params)))
        end-of-day (jt/plus date (jt/days 1))
        results (map second (biff/q db '{:find     [d (pull r [*
                                                               {:result/type
                                                                [*
                                                                 {:result/workout [*]}
                                                                 {:result/movement [*]}
                                                                 {:result-set/_parent [*]}]}])]
                                         :in       [[user start end]]
                                         :where    [[r :result/user user]
                                                    [r :result/date d]
                                                    [(>= d start)]
                                                    [(< d end)]]
                                         :order-by [[d :desc]]}
                                    [(:uid session) (->date date) (->date end-of-day)]))]
    (if (not-empty results)
      [:div.px-4.py-10.sm:px-6
       {:id "mobile-day-view"}
       [:h2.mb-4 (jt/format "EEE, MMMM dd" date)]
       [:ol.divide-y.divide-gray-100.overflow-hidden.rounded-lg.bg-white.text-sm.shadow.ring-1.ring-black.ring-opacity-5
        (map (comp (fn [{:keys [name href]}]
                     [:li.group.flex.p-4.pr-6.focus-within:bg-gray-50.hover:bg-gray-50
                      [:div.flex-auto
                       [:p.font-semibold.text-gray-900 name]]
                      [:a.ml-6.flex-none.self-center.rounded-md.bg-white.px-3.py-2.font-semibold.text-gray-900.shadow-sm.ring-1.ring-inset.ring-gray-300.hover:ring-gray-400 {:href href} "View"
                       [:span.sr-only (str ", " name)]]]) r/normalized-result) results)]]
      [:div.px-4.py-10.sm:px-6.lg:hidden
       {:id "mobile-day-view"}
       [:h2.mb-4 (jt/format "EEE, MMMM dd" date)]
       [:div.flex.flex-col.items-center
        [:p "Log a WOD result to see it show up here!"]
        [:a.btn.mb-4 {:href "/app/workouts"} "View Workouts"]
        [:a.btn {:href "/app/movements"} "View Movements"]]])))


(defn desktop-body
  [{:keys [date today results]}]
  (let [results-by-month-day (group-by (comp inst->month-day :result/date) results)]
    [:div.shadow.ring-1.ring-black.ring-opacity-5.flex.flex-auto.flex-col
     {:id "calendar"}
     [:div.grid.grid-cols-7.gap-px.border-b.border-gray-300.bg-gray-200.text-center.text-xs.font-semibold.leading-6.text-gray-700.lg:flex-none
      [:div.flex.justify-center.bg-white.py-2
       [:span "S"]
       [:span.sr-only.sm:not-sr-only "un"]]
      [:div.flex.justify-center.bg-white.py-2
       [:span "M"]
       [:span.sr-only.sm:not-sr-only "on"]]
      [:div.flex.justify-center.bg-white.py-2
       [:span "T"]
       [:span.sr-only.sm:not-sr-only "ue"]]
      [:div.flex.justify-center.bg-white.py-2
       [:span "W"]
       [:span.sr-only.sm:not-sr-only "ed"]]
      [:div.flex.justify-center.bg-white.py-2
       [:span "T"]
       [:span.sr-only.sm:not-sr-only "hu"]]
      [:div.flex.justify-center.bg-white.py-2
       [:span "F"]
       [:span.sr-only.sm:not-sr-only "ri"]]
      [:div.flex.justify-center.bg-white.py-2
       [:span "S"]
       [:span.sr-only.sm:not-sr-only "at"]]]
     [:div.flex.bg-gray-200.text-xs.leading-6.text-gray-700.lg:flex-auto
      [:div.hidden.w-full.lg:grid.lg:grid-cols-7.lg:grid-rows-6.lg:gap-px
       (map #(desktop-day {:day % :now today :date date :results (get results-by-month-day (jt/as % :month-of-year :day-of-month))}) (monthly-frame-days date))]
      [:div.isolate.grid.w-full.grid-cols-7.grid-rows-6.gap-px.lg:hidden
       (map #(mobile-day {:day % :now today :date date :results (get results-by-month-day (jt/as % :month-of-year :day-of-month))}) (monthly-frame-days date))]]
     [:div.px-4.py-10.sm:px-6.lg:hidden
      {:id "mobile-day-view"}
      [:h2.mb-4 (jt/format "EEE, MMMM dd" date)]
      [:div.flex.flex-col.items-center
       [:p "Log a WOD or movement result to see it show up here!"]
       [:a.btn.mb-4 {:href "/app/workouts"} "View Workouts"]
       [:a.btn {:href "/app/movements"} "View Movements"]]]]))


(defn calendar
  [{:keys [_date _today _results] :as params}]
  (container
    (header params)
    (desktop-body params)))
