(ns com.spicy.results.transform
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.movements.core :refer [sets-n-reps]]
    [com.spicy.results.score :refer [REPS_MULTIPLIER]]
    [com.spicy.time :as t]))


(defn ->rounds-reps
  [reps-per-round score]
  (if (int? score)
    (if (> 0 score)
      "0+0"
      (str (quot score reps-per-round)
           "+"
           (rem score reps-per-round)))
    (let [rounds (.intValue score)
          reps (.intValue (* REPS_MULTIPLIER (- (bigdec score) rounds)))]
      (str rounds "+" reps))))


(defn display-score
  [{:keys [result-set workout]}]
  (case (:workout/scheme workout)
    :rounds-reps (->rounds-reps (:workout/reps-per-round workout) (:result-set/score result-set))
    :time (t/->time (:result-set/score result-set))
    (:result-set/score result-set)))


(defn merge-set-score-with
  [sets f]
  (apply (partial merge-with f) (map #(select-keys % [:result-set/score]) sets)))


(defn display-summed-score
  [{:keys [workout sets] :as _a}]
  (when (and (not-empty workout)
             (not-empty sets))
    (display-score (merge {:workout workout} {:result-set (merge-set-score-with sets +)}))))


(defn normalized-result
  [result]
  (let [workout     (-> result :result/type :result/workout)
        movement    (-> result :result/type :result/movement)
        sets        (-> result :result/type :result-set/_parent)
        notes       (-> result :result/type :result/notes)
        description (if (seq workout)
                      (-> workout :workout/description)
                      (str (sets-n-reps sets)))
        score       (if (seq workout)
                      (display-summed-score {:workout workout
                                             :sets    sets})
                      (let [best-set (first
                                       (sort-by :result-set/weight >
                                                (filter
                                                  #(= :pass (:result-set/status %)) sets)))]
                        (str (:result-set/reps best-set) "x" (:result-set/weight best-set))))
        name        (or (some-> movement
                                :movement/name
                                string/capitalize
                                (str " (" (sets-n-reps sets) ")"))
                        (:workout/name workout))
        href        (if (:workout/name workout)
                      (str "/app/workouts/" (:xt/id workout))
                      (str "/app/movements/" (:xt/id movement)))]
    {:workout     workout
     :movement    movement
     :sets        sets
     :href        href
     :name        name
     :description description
     :notes       notes
     :score       score
     :date        (biff/format-date (:result/date result) "YYYY-MM-dd")}))
