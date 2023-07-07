(ns com.spicy.results.ui
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.numbers :as n]))


(defn result-ui
  [{{:result/keys [score workout]} :result/type
    :as result}]
  [:div#result-ui
   [:div
    [:a {:href (str "/app/workouts/" (string/lower-case (:workout/name workout)))} (:workout/name workout)]]
   [:div score]
   [:button.btn {:hx-get (str "/app/results/" (:xt/id result) "/edit")
                 :hx-target "closest #result-ui"} "Edit"]])


(defn scheme-forms
  [{:keys [workout score]}]
  (case (:workout/scheme workout)
    :time  (let [[minutes seconds] (string/split (or score ":") #":")]
             [:div.flex.gap-3
              [:input.w-full.pink-input.teal-focus#minutes
               {:type        "number"
                :name        "minutes"
                :placeholder "Minutes"
                :value       minutes
                :min         0
                :required    true}]
              [:input.w-full.pink-input.teal-focus#seconds
               {:type        "number"
                :name        "seconds"
                :placeholder "Seconds"
                :value       seconds
                :min         0
                :max         60
                :required    true}]])
    :time-with-cap "todo"
    :pass-fail     [:input.w-full#reps
                    {:type "text" :name "reps" :placeholder "Rounds Successfully Completed" :value score}]
    :rounds-reps
    (let [[rounds reps] (string/split (or score "+") #"\+")]
      [:div.flex.gap-3
       [:input.w-full.pink-input.teal-focus#rounds
        {:type "number" :name "rounds" :placeholder "Rounds" :value rounds :min 0 :required true}]
       [:input.w-full.pink-input.teal-focus#reps
        {:type "number" :name "reps" :placeholder "Reps" :value reps :min 0 :required true}]])
    [:input.w-full#reps {:type "number" :name "reps" :placeholder "Reps" :value score :min 0 :required true}]))


(defn result-form
  [{:keys [result workout action hidden hx-key form-props]} & children]
  (biff/form
    (merge (or form-props {})
           {(or hx-key :action) action
            :class              "flex flex-col gap-3"
            :hidden             hidden})
    (scheme-forms (assoc {}
                         :score (-> result :result/type :result/score)
                         :workout (or workout (-> result :result/type :result/workout))))
    [:input.pink-input.teal-focus
     {:type  "date"
      :name  "date"
      :value (biff/format-date
               (or (:result/date result) (biff/now)) "YYYY-MM-dd")}]
    [:div.flex.gap-2.items-center
     [:div.flex-1.flex.gap-2.items-center
      [:input#rx {:type     "radio"
                  :name     "scale"
                  :value    "rx"
                  :required true
                  :checked  (= (:result/scale result) :rx)}]
      [:label {:for "rx"} "Rx"]]
     [:div.flex-1.flex.gap-2.items-center
      [:input#scaled {:type     "radio"
                      :name     "scale"
                      :value    "scaled"
                      :required true
                      :checked  (= (:result/scale result) :scaled)}]
      [:label {:for "scaled"} "Scaled"]]
     [:div.flex-1.flex.gap-2.items-center
      [:input#rx+ {:type     "radio"
                   :name     "scale"
                   :value    "rx+"
                   :required true
                   :checked  (= (:result/scale result) :rx+)}]
      [:label {:for "rx+"} "Rx+"]]]
    [:textarea.w-full.pink-input.teal-focus#notes {:name        "notes"
                                                   :placeholder "notes"
                                                   :value       (:result/notes result)}]
    children))


(defn time->score
  [{:keys [minutes seconds]}]
  (+ (or (n/safe-parse-int seconds) 0)
     (* 60 (or (n/safe-parse-int minutes) 0))))


(defn reps-rounds->score
  [{:keys [reps rounds reps-per-round]}]
  (+ (or (n/safe-parse-int reps) 0)
     (* reps-per-round (n/parse-int rounds))))


(defn params->score
  [{:keys [minutes seconds rounds reps] :as params}]
  (cond
    (or (some? minutes) (some? seconds)) (time->score params)
    (and (some? rounds) (some? reps)) (reps-rounds->score params)
    (some? reps) (n/parse-int reps)
    :else nil))


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
  ;; => 42
  )
