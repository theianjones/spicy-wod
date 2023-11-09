(ns com.spicy.sugarwod.workouts
  (:require
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [com.spicy.numbers :as n]
    [com.spicy.sugarwod.transform :as t]))


(def girls (json/decode (slurp (io/resource "sugar_girls.json")) keyword))
(def heroes (json/decode (slurp (io/resource "sugar_heroes.json")) keyword))
(def movements (json/decode (slurp (io/resource "sugar_movements.json")) keyword))


(def sg->sp
  {"load" :load
   "reps" :reps
   "rounds + reps" :rounds-reps
   "time" :time})


;; => {:type "benchmarks",
;;     :id "CsUbCjnosY",
;;     :attributes
;;     {:name "Ellen",
;;      :description
;;      "3 rounds for time of:\n\n20 burpees\n21 dumbbell snatches\n12 dumbbell thrusters\n\nUse a single dumbbell on the snatches and a pair for the thrusters.\n\n♀ 35-lb. DBs ♂ 50-lb. DBs",
;;      :category "girls",
;;      :score_type "time",
;;      :movement_ids ["c94XVoGakB" "vXkMU7wdwO" "e1XFB3PF7Q"]},
;;     :links {}}

;; => {:type "benchmarks",
;;     :id "oYvBEQsXM7",
;;     :attributes
;;     {:name "MOORE",
;;      :description
;;      "Complete as many rounds as possible in 20 minutes of:\n• 15 ft Rope Climb, 1 ascent\n• Run 400 meters\n• Max rep Handstand push-ups\n\nLog HSPUs completed for each round in your notes.",
;;      :category "heroes",
;;      :score_type "rounds + reps",
;;      :movement_ids ["yJIlK7TN6q" "aPz3tX2Uwh" "50I5upgDJb"]},
;;     :links {}}
;; => {:type "benchmarks",
;;     :id "Pr8nOwChsB",
;;     :attributes
;;     {:name "Cindy",
;;      :description
;;      "AMRAP 20 minutes of:\n• 5 pull-ups\n• 10 push-ups\n• 15 squats",
;;      :category "girls",
;;      :score_type "rounds + reps",
;;      :movement_ids ["iSu93oACwo" "9iu4RgWSgi" "Q45eeXwOvG"]},
;;     :links {}}
;; => {:type "benchmarks",
;;     :id "ECmW2ebUIT",
;;     :attributes
;;     {:name "Chelsea",
;;      :description
;;      "Each minute on the minute for 30 minutes:\n• 5 pull-ups\n• 10 push-ups\n• 15 squats",
;;      :category "girls",
;;      :score_type "rounds + reps",
;;      :movement_ids ["iSu93oACwo" "9iu4RgWSgi" "Q45eeXwOvG"]},
;;     :links {}}
;; => {:type "benchmarks",
;;     :id "GciwR9meKO",
;;     :attributes
;;     {:name "Mary",
;;      :description
;;      "AMRAP in 20 minutes of:\n• 5 HSPU\n• 10 Pistol squats\n• 15 Pull-ups",
;;      :category "girls",
;;      :score_type "rounds + reps",
;;      :movement_ids ["aPz3tX2Uwh" "hJS94DdaBx" "iSu93oACwo"]},
;;     :links {}}

(defn ->reps-in-round
  [description]
  (apply + (map n/parse-int (re-seq #"\d+" (last (string/split description #":"))))))


(def sgw->spw
  {:workout/name [:attributes :name string/lower-case]
   :workout/sugar-id [:id]
   :workout/scheme [:attributes :score_type (fn [type] (get sg->sp type))]
   :workout/description [:attributes :description]})


(defn ->spicy
  [sugar-wod]
  (t/transformer sgw->spw sugar-wod))
