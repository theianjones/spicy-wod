(ns com.spicy.workouts.ui
  (:require
    [com.biffweb :as biff :refer [q]]
    [com.spicy.time :as t]
    [com.spicy.ui :as ui]))


(defn ->rounds-reps
  [reps-per-round reps]
  (if (> 0 reps)
    "0+0"
    (str (quot reps reps-per-round)
         "+"
         (rem reps reps-per-round))))


(comment
  (->rounds-reps 30 60)
  (->rounds-reps 30 61)
  (->rounds-reps 30 300)
  (->rounds-reps 30 301)
  (->rounds-reps 30 329)
  (->rounds-reps 30 330))


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
  [{:keys [workout sets] :as a}]
  (when (and (not-empty workout)
             (not-empty sets))
    (display-score (merge {:workout workout} {:result-set (merge-set-score-with sets +)}))))


(defn workout-results
  [{:keys [user workout biff/db]}]
  (let [results (biff/q db '{:find  (pull result [* {:result/type [*
                                                                   {:result-set/_parent [*]}
                                                                   {:result/workout [*]}]}])
                             :in    [[user-id workout-id]]
                             :where [[result :result/user user-id]
                                     [result :result/type wod]
                                     [wod :result/workout workout-id]]}
                        [user workout])]
    [:div {:class (str "flex flex-col relative h-full md:min-h-[60vh] border-2 border-black p-8 m-4 bg-white ")}
     [:div {:class "absolute h-full -z-10 overflow-visible inset-0 bg-[url(/img/grid.svg)] bg-center "}]
     [:h2.text-3xl "Log Book"]
     (if (zero? (count results))
       [:p {:class (str " w-fit m-auto ")} "Log a workout to see your history!"]
       [:ul.list-none.list-inside.gap-3.pl-0.ml-0
        (map (fn [{{:result/keys [scale notes workout]
                    sets         :result-set/_parent} :result/type
                   :result/keys               [date]}]
               [:li {:class (str "w-1/2")}
                [:div.flex.gap-3.flex-col
                 [:.flex.justify-between.flex-wrap.gap-2
                  [:div.text-2xl.font-bold.self-center (display-summed-score {:workout workout :sets sets})
                   [:span.pl-2.font-normal (name scale)]]
                  [:div.self-center (biff/format-date
                                      date "EEE, YYYY-MM-dd")]]
                 (when notes [:div notes])]]) results)])]))


(defn workout-logbook-ui
  [{:xt/keys [id] :workout/keys [name description scheme user] :keys [children class current-user]}]
  [:div
   {:class (str
             "flex flex-col gap-3 mx-auto text-center sm:text-left "
             "sm:w-[354px] p-8 pb-0 sm:pb-6 "
             (or class ""))}
   [:div.flex.flex-col.w-full
    [:div.flex.justify-between.flex-col.gap-4
     (when (= user current-user)
       [:a {:href  (str "/app/workouts/" id "/edit")
            :class (str "btn border bg-opacity-0 text-sm font-normal h-fit w-fit mx-auto sm:mx-0")} "Edit"])
     [:h2.text-3xl.cursor-default name]]
    [:div.py-1.cursor-default.mt-4 (ui/display-scheme scheme)]]
   [:p.whitespace-pre-wrap.sm:text-left description]
   (when (some? children)
     children)
   [:a {:href  (str "/app/results/new?workout=" id)
        :class (str "btn h-fit w-fit mx-auto sm:mx-0 bg-brand-teal ")} "Log workout"]
   [:a {:href  (str "/app/workouts/" id "/share")
        :class (str "btn h-fit w-fit mx-auto sm:mx-0 bg-brand-background ")} "Share"]])


(defn workout-form
  [{:keys [hidden workout]}]
  (biff/form
    (merge {:class     "flex flex-col gap-4"
            :hidden    hidden
            :hx-target :body}
           (if (nil? workout)
             {:hx-post "/app/workouts"}
             {:hx-put (str "/app/workouts/" (:xt/id workout))}))
    [:div.flex.flex-col.w-full
     [:label {:for :name} "Title"]
     [:input#name
      {:placeholder "Name"
       :name        "name"
       :class       (str "pink-input p-2 outline-none focus-teal")
       :required    true
       :value       (:workout/name workout)}]]
    [:div.flex.flex-col.w-full
     [:label {:for :description} "Description"]
     [:textarea.pink-input.h-48.row-5.teal-focus#description
      {:placeholder "Description"
       :name        "description"
       :required    true
       :value       (:workout/description workout)}]]
    [:div.flex.flex-col.w-full
     [:label {:for :scheme} "Scheme"]
     [:select.pink-input.teal-focus#scheme
      {:name      "scheme"
       :required  true
       :hx-get    "/app/workouts/new/scheme-inputs"
       :hx-target "#scheme-inputs"
       :hx-swap   "outerHTML"
       :value     (when (not (nil? workout)) (name (:workout/scheme workout)))}
      [:option {:value "" :label "--Select a Workout Scheme--"}]
      [:option {:value "time"
                :label "time"}]
      [:option {:value "rounds-reps"
                :label "rounds-reps"}]
      [:option {:value "reps"
                :label "reps"}]
      [:option {:value "emom"
                :label "emom"}]
      [:option {:value "load"
                :label "load"}]
      [:option {:value "calories"
                :label "calories"}]
      [:option {:value "meters"
                :label "meters"}]
      [:option {:value "feet"
                :label "feet"}]
      [:option {:value "points"
                :label "points"}]]]
    (if (= :rounds-reps (:workout/scheme workout))
      [:div#scheme-inputs
       [:div.flex.flex-col.w-full
        [:label {:for :reps-per-round} "Reps per round"]
        [:input.w-full.pink-input.p-2.teal-focus#reps-per-round
         {:placeholder "Reps per round" :name "reps-per-round" :required true}]]]
      [:div.hidden#scheme-inputs])
    [:div.flex.gap-3.items-center
     [:label "Score rounds separately?"]
     [:input#score-separately
      {:name      "score-separately"
       :type      :checkbox
       :hx-get    "/app/workouts/new/score-separately"
       :hx-target "#rounds"
       :hx-swap   "outerHTML"
       :checked   (not (nil? (:workout/rounds-to-score workout)))}]]
    (if (not (nil? (:workout/rounds-to-score workout)))
      [:div#rounds
       (when (not (nil? (:workout/rounds-to-score workout)))
         [:div.flex.flex-col.w-full
          [:label {:for :rounds} "Rounds to score"]
          [:input.w-full.pink-input.p-2.teal-focus#rounds
           {:placeholder "Rounds to score"
            :name        "rounds"
            :required    true
            :value       (:workout/rounds-to-score workout)}]])]
      [:div.hidden#rounds])
    [:div
     [:div.flex.flex-col.w-full
      [:label {:for :search} "Search and tag workout with movements"
       [:input.pink-input.teal-focus.w-full
        {:name        "search"
         :type        "search"
         :id          "search"
         :placeholder "Search for Movements..."
         :hx-get      "/app/workouts/new/search"
         :hx-trigger  "keyup changed delay:500ms, search"
         :hx-target   "#search-results"}]]]]
    [:div#selected-movements {:hx-trigger :load
                              :hx-target  :this
                              :hx-get     "/app/workouts/new/selected"}]
    [:div#search-results]
    [:button.btn.bg-brand-teal {:type "submit"} (if (nil? workout) "Create Workout" "Update Workout")]))


(defn workouts-list
  [{:keys [workouts id]}]
  [:div.flex.gap-2.sm:gap-4.flex-wrap.pb-20
   {:id id}
   (map ui/workout-ui workouts)])
