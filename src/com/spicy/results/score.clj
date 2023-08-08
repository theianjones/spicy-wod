(ns com.spicy.results.score
  (:require
    [clojure.string :as string]
    [com.spicy.numbers :as n]
    [com.spicy.route-helpers :refer [->key]]))


(defn time->score
  [{:keys [minutes seconds]}]
  (+ (or (n/safe-parse-int seconds) 0)
     (* 60 (or (n/safe-parse-int minutes) 0))))


(defn reps->score
  [{:keys [reps]}]
  (or (n/safe-parse-int reps) 0))


(defn rounds-reps->score
  [{:keys [rounds reps-per-round] :as params}]
  (+ (reps->score params)
     (* reps-per-round (n/parse-int rounds))))


(defn params->score
  [{:keys [minutes seconds rounds reps] :as params}]
  (cond
    (or (some? minutes) (some? seconds)) (time->score params)
    (and (some? rounds) (some? reps)) (rounds-reps->score params)
    (some? reps) (n/parse-int reps)
    :else nil))


(def scheme->score-params
  "The form defaults to reps so we do too.
  This generally means that we havent implemented the
  scheme for results yet"
  {:time          ["minutes" "seconds"]
   :rounds-reps   ["rounds" "reps"]
   :reps          ["reps"]
   :time-with-cap ["reps"]
   :pass-fail     ["reps"]
   :emom          ["reps"]
   :load          ["reps"]
   :calories      ["reps"]
   :meters        ["reps"]
   :feet          ["reps"]
   :points        ["reps"]})


(def default-score-params
  ["notes" "id"])


(defn ->score
  [{:keys [n scheme] :as params}]
  (let [key-builder (partial ->key n)
        score-params (concat default-score-params (scheme scheme->score-params))
        score-keys (map key-builder score-params)]
    (map (fn [key]
           (let [[key-name index] (string/split (name key) #"-")]
             {(keyword key-name) (key params)
              :index (n/parse-int index)}))
         score-keys)))


(defn ->scores
  [{:keys [rounds-to-score reps-per-round scheme]
    :as params}]
  (->> (range 0 (or rounds-to-score 1))
       (mapcat #(->score (assoc params :n %)))
       (group-by :index)
       vals
       (map #(apply merge %))
       (mapv #(if (= scheme :rounds-reps)
                (assoc % :reps-per-round reps-per-round)
                %))))


(defn scores->tx
  [{:keys [op parent]}]
  (map (fn [{:keys [index id] :as score}]
         (merge {:db/op op
                 :db/doc-type :wod-set
                 :result-set/parent parent
                 :result-set/number index
                 :result-set/score (params->score score)
                 :result-set/notes (:notes score)}
                (when id
                  {:xt/id (if (uuid? id)
                            id
                            (parse-uuid id))})))))


(comment
  (params->score {:seconds "45"})
  ;; => 45
  (params->score {:minutes "3" :seconds "45"})
  ;; => 225
  (params->score {:rounds "20" :rounds-multiplier 30})
  ;; => nil

  (params->score {:rounds "20" :reps "5" :rounds-multiplier 30})
  ;; => 605
  (params->score {:reps "42"})

  (def p
    {:date    "2023-07-14",
     :notes-1 "fell off my pace",
     :scale   "rx",
     :reps-0  "116",
     :reps-2  "96",
     :reps-1  "102",
     :workout "a02b0fe4-92f3-4044-af89-af028238b9f3",
     :notes-2 "couldnt think anymore",
     :notes   "",
     :scheme :reps
     :__anti-forgery-token
     "idW9AN+yI4rPRh40QkJ+Mz0duX5CxJforNpmO/UzS7khGITLpRVx0/ePsk7PD5bUM46u2V5NwQnyYjWO",
     :notes-0 ""})

  (->scores (assoc p :rounds-to-score 3))
  ;; => ({:reps "116", :index "0", :notes ""}
  ;;     {:reps "102", :index "1", :notes "fell off my pace"}
  ;;     {:reps "96", :index "2", :notes "couldnt think anymore"})

  (def p-2
    {:rounds-to-score 3
     :reps-per-round 100
     :date            "2023-07-14",
     :notes-1         "fell off my pace",
     :scale           "rx",
     :reps-0          "116",
     :rounds-0        "3"
     :reps-2          "96",
     :reps-1          "102",
     :rounds-2        "1"
     :rounds-1        "2"
     :workout         "a02b0fe4-92f3-4044-af89-af028238b9f3",
     :notes-2         "couldnt think anymore",
     :notes           "",
     :scheme          :rounds-reps
     :__anti-forgery-token
     "idW9AN+yI4rPRh40QkJ+Mz0duX5CxJforNpmO/UzS7khGITLpRVx0/ePsk7PD5bUM46u2V5NwQnyYjWO",
     :notes-0         ""})

  (->scores p-2)
  ;; => ({:rounds "3", :index "0", :reps "116", :notes "", :reps-to-round 100}
  ;;     {:rounds "2",
  ;;      :index "1",
  ;;      :reps "102",
  ;;      :notes "fell off my pace",
  ;;      :reps-to-round 100}
  ;;     {:rounds "1",
  ;;      :index "2",
  ;;      :reps "96",
  ;;      :notes "couldnt think anymore",
  ;;      :reps-to-round 100})

  ;; => ({:rounds "3", :index "0", :reps "116", :notes ""}
  ;;     {:rounds "2", :index "1", :reps "102", :notes "fell off my pace"}
  ;;     {:rounds "1", :index "2", :reps "96", :notes "couldnt think anymore"})

  (def p-3
    {:rounds-to-score 3
     :date            "2023-07-14",
     :notes-1         "fell off my pace",
     :scale           "rx",
     :minutes-0       "16",
     :seconds-0       "33"
     :minutes-1       "102",
     :seconds-1       "2"
     :minutes-2       "96",
     :seconds-2       "1"
     :workout         "a02b0fe4-92f3-4044-af89-af028238b9f3",
     :notes-2         "couldnt think anymore",
     :notes           "",
     :scheme          :time
     :__anti-forgery-token
     "idW9AN+yI4rPRh40QkJ+Mz0duX5CxJforNpmO/UzS7khGITLpRVx0/ePsk7PD5bUM46u2V5NwQnyYjWO",
     :notes-0         ""})

  (->scores p-3)
  ;; => ({:minutes "16", :index "0", :seconds "33", :notes ""}
  ;;     {:minutes "102", :index "1", :seconds "2", :notes "fell off my pace"}
  ;;     {:minutes "96", :index "2", :seconds "1", :notes "couldnt think anymore"})
  ;;
  (transduce (scores->tx {:op :create :parent :db.id/wod-result}) conj (->scores x)))
