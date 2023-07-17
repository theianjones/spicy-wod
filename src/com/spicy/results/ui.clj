(ns com.spicy.results.ui
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]))


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
  [{:keys [workout score identifier]
    :or   {identifier 0}}]
  (case (:workout/scheme workout)
    :time          (let [[minutes seconds] (string/split (or score ":") #":")]
                     [:div.flex.gap-3
                      [:input.w-full.pink-input.teal-focus
                       {:type        "number"
                        :name        (str "minutes-" identifier)
                        :id          (str "minutes-" identifier)
                        :placeholder "Minutes"
                        :value       minutes
                        :min         0
                        :required    true}]
                      [:input.w-full.pink-input.teal-focus
                       {:type        "number"
                        :name        (str "seconds-" identifier)
                        :id          (str "seconds-" identifier)
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
       [:input.w-full.pink-input.teal-focus
        {:type        "number"
         :name        (str "rounds-" identifier)
         :id          (str "rounds-" identifier)
         :placeholder "Rounds"
         :value       rounds
         :min         0
         :required    true}]
       [:input.w-full.pink-input.teal-focus
        {:type        "number"
         :name        (str "reps-" identifier)
         :id          (str "reps-" identifier)
         :placeholder "Reps"
         :value       reps
         :min         0
         :required    true}]])
    [:input.w-full.pink-input.teal-focus {:type        "number"
                    :name        (str "reps-" identifier)
                    :id          (str "id-" identifier)
                    :placeholder "Reps"
                    :value       score
                    :min         0
                    :required    true}]))


(defn result-form
  [{:keys [result workout action hidden hx-key form-props]} & children]
  (let [rounds-to-score (or (:workout/rounds-to-score workout) 1)]
    (biff/form
      (merge (or form-props {})
             {(or hx-key :action) action
              :class              "flex flex-col gap-3"
              :hidden             hidden})

      (if (= 1 rounds-to-score)
        (scheme-forms (assoc {}
                             :score (-> result :result/type :result/score)
                             :workout (or workout (-> result :result/type :result/workout))))
        [:ul.list-none.p-0.m-0
         (map (fn [i]
                [:li.flex.gap-3.mb-3
                 [:p.m-0.w-2 (str (inc i) ".")]
                 (scheme-forms (assoc {}
                                      :workout (or workout (-> result :result/type :result/workout))
                                      :identifier i))
                 [:input.w-full.pink-input.teal-focus {:name (str "notes-" i)
                                                       :id (str "notes-" i)
                                                       :placeholder "Notes"}]]) (range 0 rounds-to-score))])
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
      children)))
