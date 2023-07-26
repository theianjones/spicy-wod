(ns com.spicy.results.ui
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.movements.core :refer [movement-results-ui]]
    [com.spicy.workouts.ui :refer [display-summed-score]]))


(defn normalized-result
  [result]
  (let [workout     (-> result :result/type :result/workout)
        movement    (-> result :result/type :result/movement)
        sets        (-> result :result/type :result-set/_parent)
        notes       (-> result :result/type :result/notes)
        description (-> workout :workout/description)
        name        (or (:movement/name movement)
                        (:workout/name workout))]
    {:workout     workout
     :movement    movement
     :sets        sets
     :name        name
     :description description
     :notes       notes
     :date        (biff/format-date (:result/date result) "YYYY-MM-dd")}))


(defn result-ui
  [result]
  (let [{:keys [workout movement description sets name notes date]} (normalized-result result)]
    [:div.flex.justify-between.max-w-sm.sm:max-w-xl.mx-auto#result-ui
     [:div.flex.flex-col.gap-2
      [:a.text-2xl.font-bold {:href (str "/app/workouts/" (string/lower-case name))} name]
      [:p.whitespace-pre-wrap.sm:text-left.max-w-xs.text-gray-700.mb-0 date]
      [:div.flex.justify-between
       [:p.hidden.sm:block.whitespace-pre-wrap.sm:text-left.max-w-xs.text-gray-700.italic description]
       [:div.ml-1.flex.flex-col.self-center.sm:hidden
        (when (some? workout)
          [:span.text-xl
           (display-summed-score {:workout workout
                                  :sets    sets})])
        (when (some? notes)
          [:span
           notes])]]]
     (when (some? movement)
       (movement-results-ui result))
     [:div.flex.gap-4.h-fit.self-center
      [:div.hidden.sm:flex.sm:flex-col.ml-1.self-center.text-right
       (when (some? workout)
         [:span.text-xl
          (display-summed-score {:workout workout
                                 :sets    sets})])
       (when (some? notes)
         [:span
          notes])]
      [:button {:hx-get    (str "/app/results/" (:xt/id result) "/edit")
                :hx-target "closest #result-ui"
                :hx-swap   "outerHTML"
                :class     (str "btn-no-shadow bg-brand-pink h-1/2 self-center text-xl font-normal ")} "Edit"]]]))


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
  (let [workout-result (:result/type result)
        w (or workout (:result/workout workout-result))
        rounds-to-score (or (:workout/rounds-to-score w) 1)]
    (biff/form
      (merge (or form-props {})
             {(or hx-key :action) action
              :class              "flex flex-col gap-3"
              :hidden             hidden})
      (if (= 1 rounds-to-score)
        [:div
         (scheme-forms (assoc {}
                              :score (display-summed-score {:workout w :sets (-> workout-result :result-set/_parent)})
                              :workout w))
         [:input {:type "hidden"
                  :name "id-0"
                  :value (:xt/id (first (-> workout-result :result-set/_parent)))}]]
        [:ul.list-none.p-0.m-0
         (map (fn [i]
                [:li.flex.gap-3.mb-3
                 [:p.m-0.w-2 (str (inc i) ".")]
                 (scheme-forms (assoc {}
                                      :workout w
                                      :score (display-summed-score {:workout workout :sets [(nth (-> workout-result :result-set/_parent) i)]})
                                      :identifier i))
                 [:input {:type "hidden"
                          :name (str "id-" i)
                          :value (:xt/id (nth (-> workout-result :result-set/_parent) i))}]
                 [:input.w-full.pink-input.teal-focus {:name        (str "notes-" i)
                                                       :id          (str "notes-" i)
                                                       :placeholder "Notes"}]]) (range 0 rounds-to-score))])
      [:input.pink-input.teal-focus
       {:type  "date"
        :name  "date"
        :value (biff/format-date
                 (or (:result/date workout-result) (biff/now)) "YYYY-MM-dd")}]
      [:div.flex.gap-2.items-center
       [:div.flex-1.flex.gap-2.items-center
        [:input#rx {:type     "radio"
                    :name     "scale"
                    :value    "rx"
                    :required true
                    :checked  (= (:result/scale workout-result) :rx)}]
        [:label {:for "rx"} "Rx"]]
       [:div.flex-1.flex.gap-2.items-center
        [:input#scaled {:type     "radio"
                        :name     "scale"
                        :value    "scaled"
                        :required true
                        :checked  (= (:result/scale workout-result) :scaled)}]
        [:label {:for "scaled"} "Scaled"]]
       [:div.flex-1.flex.gap-2.items-center
        [:input#rx+ {:type     "radio"
                     :name     "scale"
                     :value    "rx+"
                     :required true
                     :checked  (= (:result/scale workout-result) :rx+)}]
        [:label {:for "rx+"} "Rx+"]]]
      [:textarea.w-full.pink-input.teal-focus#notes {:name        "notes"
                                                     :placeholder "notes"
                                                     :value       (:result/notes workout-result)}]
      children)))
