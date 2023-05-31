(ns com.spicy.workouts.ui
  (:require
    [com.biffweb :as biff :refer [q]]
    [com.spicy.ui :as ui]))


(defn workout-results
  [{:keys [user workout biff/db]}]
  (let [results (biff/q db '{:find (pull result [*])
                             :in [[user-id workout-id]]
                             :where [[result :result/user user-id]
                                     [result :result/workout workout-id]]}
                        [user workout])]
    [:div {:class (str "flex flex-col relative h-full md:min-h-[60vh] p-8 rounded-md shadow-[-2px_-2px_0px_rgba(0,0,0,100)] m-4")}
     [:div {:class "absolute h-full rounded-md  bg-brand-background shadow-[-2px_-2px_0px_rgba(0,0,0,100)] -z-10 overflow-visible inset-0 bg-[url(/img/grid.svg)] bg-center "}]
     [:h2.text-3xl "Log Book"]
     (if (zero? (count results))
       [:p {:class (str " w-fit m-auto ")} "Log a workout to see your history!"]
       [:ul.list-none.list-inside.gap-3.pl-0.ml-0
        (map (fn [{:result/keys [score date notes scale]}]
               [:li {:class (str "w-1/2")}
                [:div.flex.gap-3.flex-col
                 [:.flex.justify-between.flex-wrap.gap-2
                  [:div.text-2xl.font-bold.self-center score
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
   [:p.whitespace-pre.text-left description]
   (when (some? children)
     children)
   [:a {:href  (str "/app/results/new?workout=" name)
        :class (str "btn h-fit w-fit")} "Log workout"]
   ])


(defn workout-form
  [{:keys [hidden]}]
  (biff/form
    {:class  "flex flex-col gap-4"
     :action "/app/workouts"
     :hidden hidden}
    [:input.pink-input.p-2.teal-focus#name {:placeholder "Name" :name "name"}]
    [:textarea.pink-input.h-48.row-5.teal-focus#description {:placeholder "Description" :name "description"}]
    [:select.pink-input.teal-focus#scheme {:name "scheme"}
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
