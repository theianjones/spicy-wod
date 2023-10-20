(ns com.spicy.sugarwod.csv
  (:require
    [camel-snake-kebab.core :as csk]
    [cheshire.core :as json]
    [clojure.data.csv :as csv]))


(def BarbellLiftEnums
  [:enum "" "Shoulder Press" "Bench Press" "Split Jerk" "Push Press" "Sotts Press" "Clean & Jerk" "Power Clean" "Hang Power Snatch" "Pendlay Row" "Good Morning" "Clean Pull" "Deadlift" "Romanian Deadlift" "Squat Clean Thruster" "Back Pause Squat" "Hang Squat Clean" "Back Squat" "Thruster" "Sumo Deadlift" "Squat Snatch" "Box Squat" "Power Snatch" "Front Squat" "Muscle Snatch" "Power Clean & Jerk" "Snatch" "Hang Squat Snatch" "Muscle Clean" "Overhead Squat" "Hang Clean" "Hang Power Clean" "Squat Clean" "Push Jerk" "Front Pause Squat" "Clean"])


(def SugarRecord
  [:map
   [[:barbell-lift BarbellLiftEnums]
    [:best-result-display :int]
    [:best-result-raw :int]
    [:date inst?]
    [:description :string]
    [:notes :string]
    [:pr [:enum "PR"]]
    [:rx-or-scaled [:enum "RX" "SCALED"]]
    [:score-type [:enum "Load" "Rounds + Reps" "Other / Text" "Reps" "Time"]]
    [:set-details :string]
    [:title :string]]])


(defn csv-data->maps
  [csv-data]
  (map zipmap
       (->> (first csv-data)               ; First row is the header
            (map csk/->kebab-case-keyword) ; Drop if you want string keys instead
            repeat)
       (rest csv-data)))


(defn decode-record
  [r]
  (assoc r :set-details (json/decode (:set-details r) keyword)))


(defn parse-sugar-csv
  [csv]
  (->> (csv/read-csv csv)
       csv-data->maps
       (map decode-record)))
