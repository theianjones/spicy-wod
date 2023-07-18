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
  [sets fn]
  (apply (partial merge-with fn) (map #(select-keys % [:result-set/score]) sets)))


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
    [:div {:class (str "flex flex-col relative h-full md:min-h-[60vh] p-8 rounded-md shadow-[-2px_-2px_0px_rgba(0,0,0,100)] m-4")}
     [:div {:class "absolute h-full rounded-md bg-brand-background shadow-[-2px_-2px_0px_rgba(0,0,0,100)] -z-10 overflow-visible inset-0 bg-[url(/img/grid.svg)] bg-center "}]
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
                  [:div.text-2xl.font-bold.self-center (display-score (merge {:workout workout} {:result-set (merge-set-score-with sets +)}))
                   [:span.pl-2.font-normal (name scale)]]
                  [:div.self-center (biff/format-date
                                      date "EEE, YYYY-MM-dd")]]
                 (when notes [:div notes])]]) results)])]))


(defn workout-logbook-ui
  [{:workout/keys [name description scheme] :keys [children class]}]
  [:div
   {:class (str
             "flex flex-col gap-3 mx-auto text-center sm:text-left "
             "w-[354px] p-8 pb-0 sm:pb-6 "
             (or class ""))}
   [:div.flex.flex-col.w-full
    [:h2.text-3xl.cursor-default name]
    [:div.py-1.cursor-default (ui/display-scheme scheme)]]
   [:p.whitespace-pre-wrap.text-left description]
   (when (some? children)
     children)
   [:a {:href  (str "/app/results/new?workout=" name)
        :class (str "btn h-fit w-fit")} "Log workout"]])


(defn workout-form
  [{:keys [hidden]}]
  (biff/form
    {:class  "flex flex-col gap-4"
     :action "/app/workouts"
     :hidden hidden}
    [:input.pink-input.p-2.teal-focus#name {:placeholder "Name" :name "name" :required true}]
    [:textarea.pink-input.h-48.row-5.teal-focus#description {:placeholder "Description" :name "description" :required true}]
    [:select.pink-input.teal-focus#scheme {:name      "scheme"
                                           :required  true
                                           :hx-get    "/app/workouts/new/scheme-inputs"
                                           :hx-target "#scheme-inputs"
                                           :hx-swap   "outerHTML"}
     [:option {:value "" :label "--Select a Workout Scheme--"}]
     [:option {:value "time"
               :label "time"}]
     [:option {:value "time-with-cap"
               :label "time-with-cap"}]
     [:option {:value "pass-fail"
               :label "pass-fail"}]
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
               :label "points"}]]
    [:div.hidden#scheme-inputs]
    [:div.flex.gap-3.items-center
     [:label "Score rounds separately?"]
     [:input#score-separately
      {:name      "score-separately"
       :type      :checkbox
       :hx-get    "/app/workouts/new/score-separately"
       :hx-target "#rounds"
       :hx-swap   "outerHTML"}]]
    [:div.hidden#rounds]
    [:div
     [:input.pink-input.teal-focus.w-full
      {:name        "search"
       :type        "search"
       :placeholder "Search for Movements..."
       :hx-get      "/app/workouts/new/search"
       :hx-trigger  "keyup changed delay:500ms, search"
       :hx-target   "#search-results"}]]
    [:div#selected-movements]
    [:div#search-results]
    [:button.btn {:type "submit"} "Create Workout"]))
