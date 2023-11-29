(ns com.spicy.results.ui
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.results.transform :as t]
    [java-time.api :as jt]))


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
  (card (t/normalized-result result)
        [:button {:hx-get    (str "/app/results/" (:xt/id result) "/edit")
                  :hx-target "closest #result-ui"
                  :hx-swap   "outerHTML"
                  :class     (str "self-end btn-no-shadow bg-white text-sm px-4 py-2 font-normal ")} "edit"]))


(defn inline-result
  [result & children]
  (let [{:keys [score date notes scale] :as _normalized} (t/normalized-result result)]
    (if (not-empty notes)
      [:div.flex.flex-col.max-w-sm.sm:max-w-xl.mx-auto#result-ui
       [:.flex.flex-row.items-baseline.justify-between.mb-3
        [:div.text-2xl.font-bold score]
        [:p.whitespace-pre-wrap.sm:text-left.max-w-xs.text-gray-700.mb-0 date]]
       [:.flex.justify-between
        [:div.flex.flex-col.gap-2
         [:div.flex.justify-between
          (when (not-empty notes)
            [:span
             notes])]]
        (when (not-empty notes)
          children)]]
      [:div.flex.gap-3.flex-col#result-ui
       [:.flex.justify-between.flex-wrap.gap-2
        [:div.text-2xl.font-bold.self-center score
         [:span.grow.pl-2.font-normal (name scale)]]
        [:div.flex.gap-2
         [:div.self-center (biff/format-date
                             (:result/date result) "EEE, YYYY-MM-dd")]
         children]]
       (when notes [:div notes])])))


(defn inline-result-ui
  [result]
  (inline-result result
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
         :max         9999
         :required    true}]])
    [:input.w-full.pink-input.teal-focus {:type        "number"
                                          :name        (str "reps-" identifier)
                                          :id          (str "id-" identifier)
                                          :placeholder (string/capitalize (name (:workout/scheme workout)))
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
                              :score (t/display-summed-score {:workout w :sets (-> workout-result :result-set/_parent)})
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
                                      :score (t/display-summed-score
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
        :value (if (:result/date result)
                 (biff/format-date
                   (:result/date result) "YYYY-MM-dd")
                 (jt/format "YYYY-MM-dd" (jt/zoned-date-time (jt/zone-id "America/Boise"))))}]
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
