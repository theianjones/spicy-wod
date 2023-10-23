(ns com.spicy.sugarwod.core
  (:require
    [com.biffweb :as biff]
    [com.spicy.sugarwod.transform :as t]))


(defn sugar-lift?
  [{:keys [barbell-lift] :as _sugar-record}]
  (not-empty barbell-lift))


(defn sugar-movements->tx-data
  [{:keys [user] :as ctx} sugarwod-csv-data]
  (let [spicy-lifts (biff/q (:biff/db ctx)
                            '{:find  (pull m [*])
                              :where [[m :movement/name]
                                      [m :movement/type :strength]]})
        sugar-lifts (filter sugar-lift? sugarwod-csv-data)]
    (filterv seq (flatten
                   (map
                     (partial t/transform-sugar-strength->tx {:user user :movements spicy-lifts})
                     sugar-lifts)))))


(comment
  (require '[com.spicy.portal :as p])
  (require '[com.spicy.repl :as r])
  (require '[com.spicy.sugarwod.csv :as s])
  (require '[clojure.java.io :as io])

  (p/open-portal)

  (s/decode-record {:description "Hang Power Clean for load: #1:  8 reps #2:  8 reps #3:  8 reps #4:  8 reps #5:  8 reps #6:  8 reps"
                    :date "08/12/2017"
                    :score-type "Load"
                    :set-details "[{\"success\":true,\"load\":115},{\"success\":true,\"load\":115},{\"success\":true,\"load\":115},{\"success\":true,\"load\":125},{\"success\":true,\"load\":125},{\"success\":true,\"load\":125}]"
                    :barbell-lift "Hang Power Clean"
                    :best-result-raw "125"
                    :title "Hang Power Clean 6x8"
                    :rx-or-scaled "RX"
                    :notes "Barbell Cycling, trying to bounce the bar out of the power position." :best-result-display "125"
                    :pr ""})

  (def e (io/reader (io/resource "sugarwod_workouts.csv")))

  (def data
    (s/parse-sugar-csv e))

  (tap> data)


  (let [ctx (r/get-context)]
    (tap>
     (sugar-movements->tx-data (assoc ctx :user r/user-b) data)))

  (biff/add-libs))
