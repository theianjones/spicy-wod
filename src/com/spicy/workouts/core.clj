(ns com.spicy.workouts.core
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.numbers :as n]
    [com.spicy.route-helpers :refer [wildcard-override]]
    [com.spicy.ui :as ui]
    [com.spicy.workouts.ui :refer [workout-logbook-ui workout-results workout-form]]))


(defn index
  [{:keys [biff/db session] :as _ctx}]
  (let [workouts (biff/q db '{:find (pull workout [*])
                              :in [[user]]
                              :where [(or (and [workout :workout/name]
                                               (not [workout :workout/user]))
                                          [workout :workout/user user])]}
                         [(:uid session)])]
    (ui/page {}
             [:div.pb-8
              (ui/panel
                [:div.flex.sm:justify-between.align-start.flex-col.sm:flex-row.gap-4.mb-14.justify-center
                 [:h1.text-5xl.w-fit.self-center.mt-8 "Workouts"]
                 [:a
                  {:class (str "text-2xl brutal-shadow btn-hover border border-radius rounded border-black py-1 px-2 h-fit text-center w-1/2 sm:w-auto self-center ")
                   :href  (str "/app/workouts/new")}
                  "Add Workout"]]

                [:div.flex.gap-2.sm:gap-4.flex-wrap.justify-center.pb-20
                 (map #(ui/workout-ui
                         (assoc %
                                :children
                                [:a.brutal-shadow.btn-hover.border.border-radius.rounded.border-black.py-1.px-2.mt-auto
                                 {:href (str "/app/workouts/" (string/lower-case
                                                                (if (:workout/user %)
                                                                  (:xt/id %)
                                                                  (:workout/name %))))}
                                 "History"]))
                      workouts)])])))


(defn show
  [{:keys [biff/db path-params session params] :as _ctx}]
  (let [workout   (first (biff/q db '{:find  (pull workout [* {:workout-movement/_workout [{:workout-movement/movement [*]}]}])
                                      :in    [[name id]]
                                      :where [(or [workout :workout/name name]
                                                  [workout :xt/id id])]}
                                 [(string/capitalize (:id path-params))
                                  (parse-uuid (:id path-params))]))
        movements (->> workout
                       :workout-movement/_workout
                       (map :workout-movement/movement))]
    (if (true? (:fragment params))
      (ui/workout-ui workout)
      (ui/page {} [:div.pb-8
                   (ui/panel [:div {:class (str "h-full flex flex-col gap-6 md:flex-row pb-20")}
                              [:div.flex.gap-2.flex-col.justify-between
                               (workout-logbook-ui workout)
                               (when (seq movements)
                                 [:ul.list-none.flex.flex-wrap.gap-2.my-0
                                  (map (fn [m]
                                         [:li.px-2.border.border-black.brutal-shadow.border-1.border-black
                                          [:a {:href (str "/app/movements/" (:xt/id m))} (:movement/name m)]]) movements)])]
                              [:.flex-1
                               (workout-results
                                 {:user    (:uid session)
                                  :workout (:xt/id workout)
                                  :biff/db db})]])]))))


(defn create
  [{:keys [biff/db path-params session params] :as ctx}]
  (let [workout-uuid      (random-uuid)
        movements         (or (into [] (vals (:movements params))) [])
        movement-ids      (flatten (into [] (biff/q db '{:find  [e]
                                                         :in    [[name ...]]
                                                         :where [[e :movement/name name]]}
                                                    movements)))
        workout           [(merge {:db/op               :create
                                   :db/doc-type         :workout
                                   :xt/id               workout-uuid
                                   :workout/name        (:name params)
                                   :workout/user        (:uid session)
                                   :workout/scheme      (keyword (:scheme params))
                                   :workout/description (:description params)}
                                  (when (:reps-per-round params) {:workout/reps-per-round (n/safe-parse-int (:reps-per-round params))}))]
        workout-movements (map (fn [movement-id]
                                 {:db/op                     :create
                                  :db/doc-type               :workout-movement
                                  :xt/id                     (random-uuid)
                                  :workout-movement/workout  workout-uuid
                                  :workout-movement/movement movement-id}) movement-ids)]
    (biff/submit-tx ctx (into [] (concat workout
                                         workout-movements)))
    {:status  303
     :headers {"location" (str "/app/workouts/" workout-uuid "?fragment=" (true? (:fragment params)))}}))


(defonce selected-movements (atom {}))


(defn update-selected-movement!
  [{:keys [user movement method]}]
  (let [movements     (get @selected-movements user)
        new-movements (into #{} (method movements movement))]
    (swap! selected-movements
           update-in [user] (constantly new-movements))))


(defn new
  [{:keys [biff/db path-params session params] :as _ctx}]
  (update-selected-movement! {:user (:uid session)
                              :method (constantly #{})
                              :movement nil})
  (let [fragment? (:fragment params)]
    (if fragment?
      (workout-form {:hidden {:fragment (str (true? fragment?))}})

      (ui/page {}
               [:div.max-w-md.mx-auto
                (ui/panel
                  [:div.p-4
                   [:h1.text-5xl.mb-14.pt-8.text-center "New Workout"]
                   (workout-form {:hidden {:fragment (str (true? fragment?))}})])]))))


(defn show-selected-movement
  [{:keys [session] :as _context}]
  [:div.flex.gap-2
   (map-indexed (fn [idx m]
                  [:div.flex.flex-row.gap-2.border.border-black.w-fit.px-2.rounded-full#selected-movement
                   [:div.w-fit.self-center m]
                   [:input {:type "hidden" :value m :name (str "movements[" idx "]")}]
                   [:button.font-display.text-lg {:hx-delete (str "/app/workouts/new/selected?movement=" m)
                                                  :hx-target "#selected-movements"} "x"]]) (get @selected-movements (:uid session)))])


(defn remove-selected-movement
  [{:keys [params session]}]
  (update-selected-movement! {:user     (:uid session)
                              :movement (:movement params)
                              :method   (fn [movements new-movement]
                                          (remove #{new-movement} movements))})
  {:status  303
   :headers {"Location" "/app/workouts/new/selected"}})


(defn set-selected-movement
  [{:keys [params session]}]
  (update-selected-movement! {:user     (:uid session)
                              :movement (:movement params)
                              :method   conj})
  {:status  303
   :headers {"Location" "/app/workouts/new/selected"}})


(defn search
  [{:keys [biff/db session params] :as ctx}]
  (let [results (biff/q db '{:find  (pull movement [*])
                             :in    [[search]]
                             :limit 10
                             :where [[movement :movement/name name]
                                     [(clojure.string/includes? name search)]]}
                        [(or (:search params) "")])]
    [:div (map (fn [{:movement/keys [name] :xt/keys [id]}]
                 [:div {:hx-post (str "/app/workouts/new/selected?movement=" name)
                        :hx-target "#selected-movements"} name]) results)]))


(defn get-scheme-inputs
  [{:keys [params] :as _ctx}]
  [:div#scheme-inputs
   (case (:scheme params)
     "rounds-reps" [:input.w-full.pink-input.p-2.teal-focus#reps-per-round
                    {:placeholder "Reps per round" :name "reps-per-round" :required true}]
     nil)])


(def routes
  ["/workouts"
   ["" {:get  index
        :post create}]
   ["/:id" {:get (wildcard-override show {:new new :selected show-selected-movement})}]
   ["/new/search" {:get search}]
   ["/new/selected" {:get    show-selected-movement
                     :post   set-selected-movement
                     :delete remove-selected-movement}]
   ["/new/scheme-inputs" {:get get-scheme-inputs}]])


(comment

  (def m {:movements {"0" "air squats"}})
  (reset! selected-movements {})

  (update-selected-movement! {:user     "user"
                              :movement "squat"
                              :method   conj})
  (update-selected-movement! {:user     "user"
                              :movement "squat"
                              :method   (fn [movements new-movement]
                                          (remove #{new-movement} movements))}))
