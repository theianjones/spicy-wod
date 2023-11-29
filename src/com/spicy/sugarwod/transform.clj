(ns com.spicy.sugarwod.transform
  (:require
    [clojure.string :as string]
    [com.spicy.calendar :as c]
    [com.spicy.numbers :as n]
    [java-time.api :as jt]))


(defn transformer
  "Takes a translation map data to transform"
  [transform-map raw-data]
  (reduce-kv (fn [m k v]
               (let [resolved
                     (cond
                       (vector? v) (if (fn? (last v))
                                     ((last v) (get-in raw-data (butlast v)))
                                     (get-in raw-data v))
                       (fn? v) (v raw-data)
                       (map? v) (transformer v raw-data)
                       :else v)]
                 (assoc m k resolved)))
             {}
             transform-map))


(def sugar-lift->spicy-lift
  {"Shoulder Press"         "strict press"
   "Bench Press"            "bench press"
   "Push Jerk"              "push jerk"
   "Split Jerk"             "split jerk"
   "Push Press"             "push press"
   "Sotts Press"            "sotts press"
   "Clean & Jerk"           "clean and jerk"
   "Power Clean"            "power clean"
   "Power Clean & Jerk"     "power clean and jerk"
   "Clean"                  "clean"
   "Muscle Clean"           "muscle clean"
   "Hang Clean"             "hang clean"
   "Hang Power Clean"       "hang power clean"
   "Clean Pull"             "clean pull"
   "Hang Squat Clean"       "hang squat clean"
   "Squat Clean"            "squat clean"
   "Squat Clean Thruster"   "squat clean thruster (cluster)"
   "Thruster"               "thruster"
   "Snatch"                 "snatch"
   "Hang Power Snatch"      "hang power snatch"
   "Snatch Grip Push Press" "snatch grip push press"
   "Snatch Pull"            "snatch pull"
   "Squat Snatch"           "squat snatch"
   "Power Snatch"           "power snatch"
   "Muscle Snatch"          "muscle snatch"
   "Hang Squat Snatch"      "hang squat snatch"
   "Pendlay Row"            "pendlay row"
   "Deadlift"               "deadlift"
   "Romanian Deadlift"      "romainian deadlift"
   "Sumo Deadlift"          "sumo deadlift"
   "Good Morning"           "good morning"
   "Back Squat"             "back squat"
   "Back Pause Squat"       "back pause squat"
   "Overhead Squat"         "overhead squat"
   "Box Squat"              "box squat"
   "Front Squat"            "front squat"
   "Front Pause Squat"      "front pause squat"})


(defn sugar-lift->xt-id
  [movements lift-str]
  (:xt/id (first (filter #(= (get sugar-lift->spicy-lift lift-str) (:movement/name %)) movements))))


(defmulti title->reps
  (fn [title]
    (cond
      (seq (re-find #"^(\D+)\s+(\d+x\d+)+$" title)) :constant
      (seq (re-find #"^(\D+)\s+((?:\d+-)+\d+)$" title)) :variable)))


(defmethod title->reps :constant
  [title]
  (when-let [[_ _ reps-str] (re-find #"^(\D+)\s+(\d+x\d+)+$" title)]
    (let [[sets reps] (string/split reps-str #"x")]
      (repeat (n/parse-int sets) (n/parse-int reps)))))


(defmethod title->reps :variable
  [title]
  (when-let [[_ _ reps-str] (re-find #"^(\D+)\s+((?:\d+-)+\d+)$" title)]
    (let [reps (string/split reps-str #"-")]
      (map n/parse-int reps))))


(defn ->instant
  [date-str]
  (->> date-str
       (jt/local-date "MM/dd/yyyy")
       c/->instant
       java.util.Date/from))


(def sugar-set->spicy-set
  {:db/doc-type       :strength-set
   :result-set/status [:success (fn [success?] (if success? :pass :fail))]
   :result-set/weight [:load n/parse-int]})


(defn map-set-details
  [{:keys [type-id] :as _opts}]
  (fn [{:keys [set-details title] :as _sugar-record}]
    (let [reps   (title->reps title)
          map-fn (fn [idx set]
                   (-> (transformer sugar-set->spicy-set set)
                       (assoc :result-set/number (inc idx)
                              :result-set/parent type-id
                              :db/op       :create
                              :result-set/reps (nth reps idx))))]
      (into [] (map-indexed map-fn set-details)))))


(defn sugar-strength->spicy-strength
  [{:keys [type-id user movements] :as opts}]
  {:strength-result {:result/movement  [:barbell-lift (partial sugar-lift->xt-id movements)]
                     :result/notes     [:notes]
                     :result/set-count [:set-details count]
                     :db/doc-type      :strength-result
                     :db/op            :create
                     :xt/id            type-id}
   :result          {:result/date [:date ->instant]
                     :db/doc-type :result
                     :db/op       :create
                     :result/type type-id
                     :result/user user}
   :sets (map-set-details opts)})


(defn transform-sugar-strength->tx
  [{:keys [_user _movements] :as ctx} sugar-record]
  (try
    (let [{:keys [strength-result result sets]}
          (transformer
            (sugar-strength->spicy-strength (assoc ctx :type-id (random-uuid)))
            sugar-record)]
      (concat [result strength-result] sets))
    (catch Exception _e
      nil)))
