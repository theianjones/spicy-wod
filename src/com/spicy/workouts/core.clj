(ns com.spicy.workouts.core
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.middleware :as mid]
    [com.spicy.numbers :as n]
    [com.spicy.route-helpers :refer [wildcard-override]]
    [com.spicy.ui :as ui]
    [com.spicy.workouts.ui :refer [workout-logbook-ui workout-results workout-form workouts-list]]))


(def SEARCH_ID "search-results")


(defn workout-search
  [{:keys [biff/db params session]}]
  ;; searching name like this will be unperformant
  ;; when an optimization needs to be made, refer to this
  ;; https://docs.xtdb.com/extensions/full-text-search/
  (let [where-clause (if (= "girls" (:type params))
                       '[(not [workout :workout/user])
                         [workout :workout/name name]
                         [(clojure.string/lower-case name) lower-name]
                         [(clojure.string/includes? lower-name search)]]
                       '[[workout :workout/user user]
                         [workout :workout/name name]
                         [(clojure.string/lower-case name) lower-name]
                         [(clojure.string/includes? lower-name search)]])
        workouts     (map second (biff/q db {:find     '[name (pull workout [*])]
                                             :in       '[[search user]]
                                             :where where-clause
                                             :order-by '[[name]]}
                                         [(string/lower-case (or (:search params) "")) (:uid session)]))]
    (workouts-list {:workouts workouts :id SEARCH_ID})))


(defn index
  [{:keys [biff/db session params] :as ctx}]
  (let [type         (or (:type params) "your-workouts")
        where-clause (if (= "girls" type)
                       '[(not [workout :workout/user])
                         [workout :workout/created-at created-at]
                         [workout :workout/name name]]
                       '[[workout :workout/user user]
                         [workout :workout/created-at created-at]
                         [workout :workout/name name]])
        workouts     (map first (biff/q db {:find     '[(pull workout [*]) created-at name]
                                            :in       '[[user]]
                                            :where    where-clause
                                            :order-by '[[created-at :desc]
                                                        [name :asc]]} [(:uid session)]))]
    (ui/page ctx
             [:div.pb-8
              (ui/panel
                [:div.flex.sm:justify-between.align-start.flex-col.sm:flex-row.gap-4.justify-center.mb-8
                 [:h1.text-5xl.w-fit.self-center.mt-8 "Workouts"]
                 (when (not (zero? (count workouts)))
                   [:a
                    {:class (str "btn bg-brand-teal h-1/2 self-center")
                     :href  (str "/app/workouts/new")}
                    "Create Workout"])]
                [:div.pb-8.flex.flex-col.sm:flex-row.justify-center.gap-4
                 [:select.btn.text-base.w-full.md:w-96.h-12.teal-focus.hover:cursor-pointer {:name     "type"
                                                                                             :onchange "window.open('?type=' + this.value,'_self')"}
                  [:option.text-base {:value    :your-workouts
                                      :selected (= type "your-workouts")} "Your Workouts"]
                  [:option.text-base {:value    :girls
                                      :selected (= type "girls")} "CrossFit Benchmarks"]]
                 [:input.pink-input.teal-focus.w-full.md:w-96
                  {:name        "search"
                   :type        "search"
                   :id          "search"
                   :placeholder "Search for workouts..."
                   :hx-get      "/app/workouts/search"
                   :hx-trigger  "keyup changed delay:500ms, search"
                   :hx-target   (str "#" SEARCH_ID)
                   :hx-swap     "outerHTML"
                   :hx-include  "[name='type']"}]]
                (if (zero? (count workouts))
                  [:div.flex.flex-col.gap-4.justify-center
                   [:p.text-center
                    "Create a workout and they will show up here"]
                   [:a
                    {:class (str "btn bg-brand-teal h-1/2 self-center")
                     :href  (str "/app/workouts/new")}
                    "Create Workout"]]
                  (workouts-list {:workouts workouts :id SEARCH_ID})))])))


(defn show
  [{:keys [biff/db path-params session params] :as ctx}]
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
      (ui/page ctx [:div.pb-8
                    (ui/panel [:div {:class (str "h-full flex flex-col gap-6 md:flex-row pb-20")}
                               [:div.flex.gap-2.flex-col.justify-between
                                (workout-logbook-ui (merge  workout {:current-user (:uid session)}))
                                (when (seq movements)
                                  [:ul.list-none.flex.flex-wrap.gap-2.my-0.sm:justify-start.justify-center.sm:pl-8.p-0.mt-4
                                   (map (fn [m]
                                          [:li.px-2.border.border-black.brutal-shadow.border-1.border-black
                                           [:a {:href (str "/app/movements/" (:xt/id m))} (:movement/name m)]]) movements)])]
                               [:.flex-1
                                (workout-results
                                  {:user    (:uid session)
                                   :workout (:xt/id workout)
                                   :biff/db db})]])]))))


(defn share
  [{:keys [biff/db path-params] :as ctx}]
  (let [workout   (first (biff/q db '{:find  (pull workout [* {:workout-movement/_workout [{:workout-movement/movement [*]}]}])
                                      :in    [[name id]]
                                      :where [(or [workout :workout/name name]
                                                  [workout :xt/id id])]}
                                 [(string/capitalize (:id path-params))
                                  (parse-uuid (:id path-params))]))]
    (ui/share-page ctx
                   [:div
                    {:class (str "max-w-sm mx-auto h-[80vh] flex flex-col")}
                    [:div.mt-8.h-full
                     [:div.flex.flex-col.gap-8
                      [:h1
                       {:class (str " font-display text-5xl text-center")}
                       [:a
                        {:href  (str "/app/workouts/" (parse-uuid (:id path-params)))}
                        (:workout/name workout)]]
                      [:div
                       {:class (str "w-fit bg-gradient-to-tr from-[#FF6750] to-[#991640] px-4 py-2 text-white font-bold self-center ")}
                       (ui/display-scheme (:workout/scheme workout))]]
                     [:div
                      {:class (str " h-[calc(100%-8rem)] flex flex-col")}
                      [:p
                       {:class (str "mt-12 text-center font-bold text-xl whitespace-pre-wrap line-clamp-[17]")}
                       (:workout/description workout)]]]

                    [:p
                     {:class (str " text-opacity-40 text-center text-xl font-display ")}
                     "spicywod.com"]])))


(defn update-page
  [{:keys [biff/db path-params params] :as ctx}]
  (let [workout-uuid          (parse-uuid (:id path-params))
        workout               (first (biff/q db '{:find  (pull w [* {:workout-movement/_workout [* {:workout-movement/movement [*]}]}])
                                                  :in    [[workout]]
                                                  :where [[w :xt/id workout]]}
                                             [workout-uuid]))
        workout-movements (-> workout
                              :workout-movement/_workout)
        existing-movement-ids (->> workout-movements
                                   (map (comp :xt/id  :workout-movement/movement))
                                   (into #{}))
        movements             (or (into [] (vals (:movements params))) [])
        movement-ids          (flatten (into [] (biff/q db '{:find  [e]
                                                             :in    [[name ...]]
                                                             :where [[e :movement/name name]]}
                                                        movements)))
        workout-tx            [(merge {:db/op               :update
                                       :db/doc-type         :workout
                                       :xt/id               workout-uuid
                                       :workout/name        (:name params)
                                       :workout/scheme      (keyword (:scheme params))
                                       :workout/description (:description params)}
                                      (when
                                        (:rounds params)
                                        {:workout/rounds-to-score
                                         (n/safe-parse-int (:rounds params))}))]
        movements-tx     (filterv (comp not nil?)
                                  (concat
                                    (map (fn [possibly-new-movement-id]
                                           (when (not (contains? existing-movement-ids possibly-new-movement-id))
                                             {:db/op                     :create
                                              :db/doc-type               :workout-movement
                                              :workout-movement/workout  workout-uuid
                                              :workout-movement/movement possibly-new-movement-id})) movement-ids)
                                    (map (fn [{:keys [workout-movement/movement xt/id] :as _wm}]
                                           (when (not (contains? (into #{} movement-ids) (:xt/id movement)))
                                             {:db/op :delete
                                              :xt/id id}))
                                         workout-movements)))]
    (biff/submit-tx ctx (concat workout-tx
                                movements-tx))
    {:status  303
     :headers {"HX-Location" (str "/app/workouts/" workout-uuid "?fragment=" (true? (:fragment params)))}}))


(defn create
  [{:keys [biff/db session params] :as ctx}]
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
                                   :workout/created-at  :db/now
                                   :workout/description (:description params)}
                                  (when
                                    (:rounds params)
                                    {:workout/rounds-to-score
                                     (n/safe-parse-int (:rounds params))}))]
        workout-movements (map (fn [movement-id]
                                 {:db/op                     :create
                                  :db/doc-type               :workout-movement
                                  :xt/id                     (random-uuid)
                                  :workout-movement/workout  workout-uuid
                                  :workout-movement/movement movement-id}) movement-ids)]
    (biff/submit-tx ctx (into [] (concat workout
                                         workout-movements)))
    {:status  303
     :headers {"HX-Location" (str "/app/workouts/" workout-uuid "?fragment=" (true? (:fragment params)))}}))


(defonce selected-movements (atom {}))


(defn update-selected-movement!
  [{:keys [user movement method]}]
  (let [movements     (get @selected-movements user)
        new-movements (into #{} (method movements movement))]
    (swap! selected-movements
           update-in [user] (constantly new-movements))))


(defn edit
  [{:keys [biff/db path-params session] :as ctx}]
  (let [workout        (first (biff/q db '{:find  (pull workout [* {:workout-movement/_workout [{:workout-movement/movement [*]}]}])
                                           :in    [[name id]]
                                           :where [(or [workout :workout/name name]
                                                       [workout :xt/id id])]}
                                      [(string/capitalize (:id path-params))
                                       (parse-uuid (:id path-params))]))
        movements      (->> workout
                            :workout-movement/_workout
                            (map :workout-movement/movement))
        movement-names (map :movement/name movements)]
    (update-selected-movement! {:user     (:uid session)
                                :method   (constantly (into #{} movement-names))
                                :movement nil})
    (ui/page ctx
             [:div.max-w-md.mx-auto
              (ui/panel
                [:a.btn.w-fit {:href (str "/app/workouts/" (:id path-params))} "Back"]
                [:div.p-4
                 [:h1.text-5xl.mb-14.pt-8.text-center (str "Update " (:workout/name workout))]
                 (workout-form {:hidden  {}
                                :workout workout})])])))


(defn new
  [{:keys [session params] :as ctx}]
  (update-selected-movement! {:user (:uid session)
                              :method (constantly #{})
                              :movement nil})
  (let [fragment? (:fragment params)]
    (if fragment?
      (workout-form {:hidden {:fragment (str (true? fragment?))}})

      (ui/page ctx
               [:div.max-w-md.mx-auto
                (ui/panel
                  [:div.p-4
                   [:h1.text-5xl.mb-14.pt-8.text-center "New Workout"]
                   (workout-form {:hidden {:fragment (str (true? fragment?))}})])]))))


(defn show-selected-movement
  [{:keys [session] :as _context}]
  [:div.flex.flex-wrap.gap-2
   (map-indexed (fn [idx m]
                  [:div.flex.flex-row.gap-2.border.border-black.w-fit.px-2#selected-movement
                   [:div.w-fit.self-center m]
                   [:input {:type "hidden" :value m :name (str "movements[" idx "]")}]
                   [:button.font-display.text-lg {:tabindex  1
                                                  :hx-delete (str "/app/workouts/new/selected?movement=" m)
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
  [{:keys [biff/db params] :as _ctx}]
  (let [results (biff/q db '{:find  (pull movement [*])
                             :in    [[search]]
                             :limit 10
                             :where [[movement :movement/name name]
                                     [(clojure.string/includes? name search)]]}
                        [(or (:search params) "")])]
    [:div (map (fn [{:movement/keys [name] :xt/keys [id]}]
                 [:div.cursor-pointer {:tabindex      0
                                       :id            (str "search-" id)
                                       :hx-post       (str "/app/workouts/new/selected?movement=" name)
                                       "@click"       "document.getElementById('search').focus()"
                                       "@keyup.enter" (format "htmx.trigger('%s', 'click')
                                                               document.getElementById('search').focus()"
                                                              (str "#search-" id))
                                       :hx-target     "#selected-movements"} name]) results)]))


(defn get-score-separately
  [{:keys [params] :as _ctx}]
  (if (= "on" (:score-separately params))
    [:div#rounds
     [:div.flex.flex-col.w-full
      [:label {:for :rounds} "Rounds to score"]
      [:input.w-full.pink-input.p-2.teal-focus#rounds
       {:placeholder "Rounds to score" :name "rounds" :required true}]]]
    [:div.hidden#rounds]))


(def routes
  ["/workouts"
   ["" {:get  index
        :post create}]
   ["/:id" {:middleware [(partial mid/wrap-ensure-owner #{"new" "selected" "search"})]}
    ["" {:get (wildcard-override show {:new      new
                                       :selected show-selected-movement
                                       :search   workout-search})
         :put update-page}]
    ["/edit" {:get edit}]
    ["/share" {:get share}]]
   ["/new/search" {:get search}]
   ["/new/selected" {:get    show-selected-movement
                     :post   set-selected-movement
                     :delete remove-selected-movement}]
   ["/new/score-separately" {:get get-score-separately}]])


(comment

  (reset! selected-movements {})

  (update-selected-movement! {:user     "user"
                              :movement "squat"
                              :method   conj})
  (update-selected-movement! {:user     "user"
                              :movement "squat"
                              :method   (fn [movements new-movement]
                                          (remove #{new-movement} movements))}))
