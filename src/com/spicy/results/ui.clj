(ns com.spicy.results.ui
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.movements.core :refer [sets-n-reps]]
    [com.spicy.workouts.ui :refer [display-summed-score]]))


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
        name        (or (:movement/name movement)
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


(defn card
  [{:keys [name href date description notes score]} & children]
  [:div.flex.flex-col.max-w-sm.sm:max-w-xl.mx-auto#result-ui
   [:.flex.flex-row.items-baseline.justify-between.mb-3
    [:a.text-2xl.font-bold {:href href} name]
    [:p.whitespace-pre-wrap.sm:text-left.max-w-xs.text-gray-700.mb-0 date]]
   [:.flex.justify-between
    [:div.flex.flex-col.gap-2
     [:div.flex.justify-between
      [:p.hidden.sm:block.whitespace-pre-wrap.sm:text-left.max-w-xs.text-gray-700.italic description]
      [:div.ml-1.flex.flex-col.self-center.sm:hidden
       (when (some? score)
         [:span.text-xl
          score])
       (when (some? notes)
         [:span
          notes])]]]
    [:div.flex.gap-4.h-fit.self-center.flex-col.justify-between
     [:div.hidden.sm:flex.sm:flex-col.ml-1.self-center.text-right
      (when (some? score)
        [:span.text-xl
         score])
      (when (some? notes)
        [:span
         notes])]
     children]]])


(defn result-ui
  [result]
  (card (normalized-result result)
        [:button {:hx-get    (str "/app/results/" (:xt/id result) "/edit")
                  :hx-target "closest #result-ui"
                  :hx-swap   "outerHTML"
                  :class     (str "self-end btn-no-shadow bg-white text-sm px-4 py-2 font-normal ")} "edit"]))


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


(defn movement-form
  [{:keys [result]}]

  (biff/form {:id        (str "expanded-" (:xt/id result))
              :class     (str "col-span-4 grid grid-cols-[1fr_1fr_1fr_minmax(30px,170px)] w-full bg-darker-brand-teal")
              :hidden    {:sets (count (-> result :result/type :result-set/_parent))}
              :hx-put    (str "/app/movements/" (-> result :result/type :result/movement) "/results/" (:xt/id result))
              :hx-swap   "outerHTML"
              :hx-target (str "#expanded-" (:xt/id result))}
             [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-l-2.border-black.text-lg.font-bold.whitespace-nowrap "Set #"]
             [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-black.text-lg.font-bold "Reps"]
             [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-black.text-lg.font-bold
              "Weight"
              [:span.font-normal.ml-2 "(edit)"]]
             [:div.px-2.py-4.border-r-2.border-b-2.border-black.flex.justify-center.items-center.font-bold
              "Hit?"
              [:span.font-normal.ml-2 "(edit)"]]
             (map (fn [{:result-set/keys [number reps weight status] :xt/keys [id]}]
                    [:<>
                     [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-l-2.border-black.text-lg number]
                     [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-black.text-lg reps]
                     [:div.px-2.py-4.border-r-2.border-b-2.border-black.text-lg
                      [:input {:name  (str "id-" number)
                               :id    (str "id-" number)
                               :type  :hidden
                               :value id}]
                      [:input {:name     (str "weight-" number)
                               :id       (str "weight-" number)
                               :class    (str "w-32 sm:w-full text-center ")
                               :required true
                               :type     :number
                               :value    weight}]]
                     [:div.flex.justify-center.items-center.w-full.px-4.sm:px-2.py-4.border-r-2.border-b-2.border-black.text-lg.capitalize
                      [:input {:name    (str "hit-miss-" number)
                               :id      (str "hit-" number)
                               :class   (str "")
                               :value   :hit
                               :type    :checkbox
                               :checked (= :pass status)}]]])
                  (sort-by :result-set/number (-> result :result/type :result-set/_parent)))
             [:div.px-2.py-6.border-r-2.border-b-2.border-l-2.border-black ""]
             [:div.px-2.py-6.border-r-2.border-b-2.border-black ""]
             [:button.p-2.text-md.grow.bg-brand-teal.sm:border-r-2.border-b-2.border-black.font-bold
              {:hx-delete (str "/app/movements/" (-> result :result/type :result/movement) "/results/" (:xt/id result))
               :hx-target (str "#expanded-" (:xt/id result))
               :hx-swap   "outerHTML"}
              "Close"]
             [:button.p-2.text-md.grow.bg-brand-blue.border-b-2.border-black.font-bold
              {:type :submit}
              "Save"]))


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
                                      :score (display-summed-score
                                               {:workout workout
                                                :sets (filter (complement nil?) [(nth (-> workout-result :result-set/_parent) i)])})
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
                 (or (:result/date result) (biff/now)) "YYYY-MM-dd")}]
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
