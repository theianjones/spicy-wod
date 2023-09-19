 (ns com.spicy.movements.core
   (:require
     [clojure.instant :as instant]
     [clojure.string :as string]
     [com.biffweb :as biff]
     [com.spicy.movements.ui :refer [movement-form strength-set-inputs]]
     [com.spicy.numbers :refer [parse-int safe-parse-int]]
     [com.spicy.route-helpers :refer [->key htmx-request? wildcard-override]]
     [com.spicy.ui :as ui]
     [xtdb.api :as xt]))


(defn movements-list
  [movements]
  [:ul#movement-list.w-full.list-none.list-inside.pl-0.flex.flex-wrap.gap-2.sm:gap-4.md:grid.md:grid-cols-2.justify-center
   (map (fn [m]
          [:li {:class (str "w-full z-[1] border-2 border-black bg-white sm:text-2xl font-bold text-black text-center whitespace-nowrap hover:brutal-shadow flex items-center justify-center ")}
           [:a.p-2.block.w-full.capitalize.whitespace-break-spaces {:href (str "/app/movements/" (:xt/id m))} (:movement/name m)]]) movements)])


(defn search
  [{:keys [biff/db params] :as _ctx}]
  (let [movement-type (or (keyword (:type params)) :strength)
        results (map second (biff/q db '{:find [name (pull movement [*])]
                                         :in    [[search type]]
                                         :where [[movement :movement/name name]
                                                 [(clojure.string/includes? name search)]
                                                 [movement :movement/type type]]
                                         :order-by [[name]]}
                                    [(or (:search params) "") movement-type]))]
    [:div.lg:mt-4.w-full.lg:mx-auto
     {:id "search-results"}
     (movements-list results)]))


(defn movement-workout-ui
  [w]
  (ui/workout-ui w))


(defn index
  [{:keys [biff/db params] :as ctx}]
  (let [movement-type (or (keyword (:type params)) :strength)
        movements (map second (biff/q db '{:find  [name (pull m [*])]
                                           :in    [[type]]
                                           :where [[m :movement/name name]
                                                   [m :movement/type type]]
                                           :order-by [[name]]}
                                      [movement-type]))]
    (if (htmx-request? ctx)
      (movements-list movements)
      (ui/page ctx (ui/panel [:div.flex.flex-col.sm:flex-row.sm:flex-wrap.gap-4.sm:justify-center
                              [:div.w-full.mt-8.flex.flex-wrap.justify-center.items-center.sm:justify-between.mb-4
                               [:h1.text-5xl.w-fit.self-center.mb-4.md:mb-0 "Movements"]
                               [:a {:class (str "btn bg-brand-teal w-full sm:w-fit self-center")
                                    :href  (str "/app/movements/new")} "Add Movement"]]
                              [:div.flex.flex-col.sm:flex-row.justify-end.gap-4
                               [:select.btn.text-base.w-full.md:w-96.h-12.teal-focus.hover:cursor-pointer {:name     "type"
                                                                                                           :onchange "window.open('?type=' + this.value,'_self')"}
                                [:option.text-base {:value    :strength
                                                    :selected (or (= (:type params) "strength") (empty? (:type params)))} "Strength"]
                                [:option.text-base {:value    :gymnastic
                                                    :selected (= (:type params) "gymnastic")} "Gymnastic"]
                                [:option.text-base {:value    :monostructural
                                                    :selected (= (:type params) "monostructural")} "Cardio"]]
                               [:input.pink-input.teal-focus.w-full.h-full.md:w-96
                                {:name        "search"
                                 :type        "search"
                                 :id          "search"
                                 :placeholder "Search for Movements..."
                                 :hx-get      "/app/movements/search"
                                 :hx-trigger  "keyup changed delay:500ms, search"
                                 :hx-target   "#search-results" :hx-swap     "outerHTML"
                                 :hx-include  "[name='type']"}]]
                              [:div.md:mt-2.w-fit.md:mx-auto
                               {:id "search-results"}
                               (movements-list movements)]])))))


(defn new
  [ctx]
  (ui/page ctx
           [:div.max-w-md.mx-auto
            (ui/panel
              [:div.p-4
               [:h1.text-5xl.mb-14.pt-8.text-center "New Movement"]
               (movement-form {})])]))


(defn create
  [{:keys [params] :as ctx}]
  (let [movement-uuid (random-uuid)
        new-movement  [{:db/op         :create
                        :db/doc-type   :movement
                        :movement/name (:name params)
                        :movement/type (keyword (:type params))
                        :xt/id         movement-uuid}]]
    (biff/submit-tx ctx new-movement)
    {:status  303
     :headers {"location" (str "/app/movements/" movement-uuid)}}))


(defn sets-n-reps
  [sets]
  (let [reps (into #{} (map :result-set/reps sets))]
    (if (= 1 (count reps))
      (string/join (interpose "x" [(count sets) (-> sets first :result-set/reps)]))
      (string/join (interpose "-" (map :result-set/reps  (sort-by :result-set/number sets)))))))


(defn movement-results-ui
  [{:result/keys [type] :as result}]
  (let [sets (:result-set/_parent type)]
    [:<>
     [:div.flex.justify-center.items-center.p-2.text-lg.border-r-2.border-l-2.border-b-2.border-black
      (biff/format-date (:result/date result) "YYYY-MM-dd")]
     [:div.p-2.text-lg.flex.justify-center.items-center.border-r-2.border-b-2.border-black
      (sets-n-reps sets)]
     [:div.p-2.flex.justify-center.items-center.text-lg.border-r-2.border-b-2.border-black
      (apply max (map #(if (= :pass (:result-set/status %))
                         (:result-set/weight %)
                         0) sets))]
     [:div.border-r-2.border-b-2.border-black.text-md.font-bold
      [:div.flex.h-full.flex-1.sm:flex-row.flex-col
       [:button.p-2.text-md.grow.bg-brand-teal.sm:border-r-2.border-b-2.border-black.sm:border-b-0

        {:hx-get (str "/app/movements/" (:result/movement type) "/results/" (:xt/id result))
         :hx-target (str "#expanded-" (:xt/id result))
         :hx-swap "outerHTML"}
        "View"]
       [:button.p-2.text-md.grow.bg-brand-blue
        {:hx-get (str "/app/movements/" (:result/movement type) "/results/" (:xt/id result) "/edit")
         :hx-target (str "#expanded-" (:xt/id result))
         :hx-swap "outerHTML"}
        "Edit"]]]
     [:div.col-span-4.hidden {:id (str "expanded-" (:xt/id result))}]]))


(defn closed-result
  [{:keys [path-params] :as _ctx}]
  [:div.col-span-4.hidden {:id (str "expanded-" (:result-id path-params))}])


(defn expanded-edit-result
  [{:keys [biff/db path-params] :as _ctx}]
  (let [result
        (first (biff/q db '{:find  (pull result [* {:result/type
                                                    [*
                                                     {:result-set/_parent [*]}]}])
                            :in    [[r]]
                            :where [[result :xt/id r]]}
                       [(parse-uuid (:result-id path-params))]))]
    [:<>
     (biff/form {:id        (str "expanded-" (:xt/id result))
                 :class     (str "col-span-4 grid grid-cols-[1fr_1fr_1fr_minmax(30px,170px)] w-full bg-slate-100")
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
                 "Save"])]))


(defn expanded-result
  [{:keys [biff/db path-params] :as _ctx}]
  (let [result
        (first (biff/q db '{:find  (pull result [* {:result/type
                                                    [*
                                                     {:result-set/_parent [*]}]}])
                            :in    [[r]]
                            :where [[result :xt/id r]]}
                       [(parse-uuid (:result-id path-params))]))]
    [:<>
     [:div.col-span-4 {:id    (str "expanded-" (:xt/id result))
                       :class "grid grid-cols-[1fr_1fr_1fr_minmax(30px,170px)] w-full bg-slate-100"}
      [:div.px-2.py-4.border-r-2.border-b-2.border-l-2.border-black.text-center.text-lg.font-bold.whitespace-nowrap "Set #"]
      [:div.px-2.py-4.border-r-2.border-b-2.border-black.text-center.text-lg.font-bold "Reps"]
      [:div.px-2.py-4.border-r-2.border-b-2.border-black.text-center.text-lg.font-bold "Weight"]
      [:div.px-2.py-4.border-r-2.border-b-2.border-black.text-center.font-bold "Hit?"]
      (map (fn [{:result-set/keys [number reps weight status]}]
             [:<>
              [:div.px-2.py-4.border-r-2.border-b-2.border-l-2.border-black.text-center.text-lg number]
              [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-black.text-lg reps]
              [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-black.text-lg weight]
              [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-black.text-lg.capitalize (name status)]])
           (sort-by :result-set/number (-> result :result/type :result-set/_parent)))
      [:div.px-2.py-6.border-r-2.border-b-2.border-l-2.border-black ""]
      [:div.px-2.py-6.border-r-2.border-b-2.border-black ""]
      [:div.px-2.py-6.border-r-2.border-b-2.border-black ""]
      [:button.p-2.text-md.grow.bg-brand-teal.sm:border-r-2.border-b-2.border-black.font-bold
       {:hx-delete (str "/app/movements/" (-> result :result/type :result/movement) "/results/" (:xt/id result))
        :hx-target (str "#expanded-" (:xt/id result))
        :hx-swap   "outerHTML"}
       "Close"]]]))


(def x '({:result/date #inst "2023-08-31T00:00:00.000-00:00", :result/user #uuid "8cdef6f4-a747-4135-aaf3-36ef2f73ce99", :result/type {:result/movement #uuid "6fffd335-f2bb-4672-ba2f-e91a14ed21e1", :result/notes "actually felt good!!!!", :result/set-count 5, :xt/id #uuid "615fa13b-fd38-4704-8d82-1157c795d296", :result-set/_parent ({:result-set/parent #uuid "615fa13b-fd38-4704-8d82-1157c795d296", :result-set/reps 5, :result-set/status :fail, :result-set/number 4, :result-set/weight 270, :xt/id #uuid "244f7fe3-6afc-4542-aeb7-48ee3cbcbff2"} {:result-set/parent #uuid "615fa13b-fd38-4704-8d82-1157c795d296", :result-set/reps 5, :result-set/status :pass, :result-set/number 5, :result-set/weight 265, :xt/id #uuid "3bddf6b2-ce7d-479a-aca0-9aa309c15167"} {:result-set/parent #uuid "615fa13b-fd38-4704-8d82-1157c795d296", :result-set/reps 5, :result-set/status :pass, :result-set/number 1, :result-set/weight 240, :xt/id #uuid "6fafc46c-3d8e-4c86-99c5-5a0e5ced7358"} {:result-set/parent #uuid "615fa13b-fd38-4704-8d82-1157c795d296", :result-set/reps 5, :result-set/status :pass, :result-set/number 2, :result-set/weight 250, :xt/id #uuid "eabf08b6-366c-41a5-b47c-ee314293a0be"} {:result-set/parent #uuid "615fa13b-fd38-4704-8d82-1157c795d296", :result-set/reps 5, :result-set/status :pass, :result-set/number 3, :result-set/weight 260, :xt/id #uuid "f5f36aa4-a5a3-4695-ab95-aa4f1326e74d"})}, :xt/id #uuid "729fd4ed-7d3c-4b5f-90bd-5817765f3d53"} {:result/date #inst "2023-08-18T00:00:00.000-00:00", :result/user #uuid "8cdef6f4-a747-4135-aaf3-36ef2f73ce99", :result/type {:result/movement #uuid "6fffd335-f2bb-4672-ba2f-e91a14ed21e1", :result/notes "", :result/set-count 5, :xt/id #uuid "32605bd0-bfb9-4fa9-ae90-7ae8df42a408", :result-set/_parent ({:result-set/parent #uuid "32605bd0-bfb9-4fa9-ae90-7ae8df42a408", :result-set/reps 3, :result-set/status :fail, :result-set/number 5, :result-set/weight 5, :xt/id #uuid "89afe3a5-4827-47a8-aaae-84f175fcab75"} {:result-set/parent #uuid "32605bd0-bfb9-4fa9-ae90-7ae8df42a408", :result-set/reps 4, :result-set/status :pass, :result-set/number 3, :result-set/weight 3, :xt/id #uuid "92434991-523b-4aae-9b2b-746ce638fe0a"} {:result-set/parent #uuid "32605bd0-bfb9-4fa9-ae90-7ae8df42a408", :result-set/reps 5, :result-set/status :pass, :result-set/number 1, :result-set/weight 1, :xt/id #uuid "98f8ef65-abf8-4061-b88e-338385891a5f"} {:result-set/parent #uuid "32605bd0-bfb9-4fa9-ae90-7ae8df42a408", :result-set/reps 4, :result-set/status :pass, :result-set/number 2, :result-set/weight 2, :xt/id #uuid "a61cc08b-a392-4e2c-9b20-63de33794bf9"} {:result-set/parent #uuid "32605bd0-bfb9-4fa9-ae90-7ae8df42a408", :result-set/reps 3, :result-set/status :pass, :result-set/number 4, :result-set/weight 4, :xt/id #uuid "f662eabf-2f50-4ff2-8448-e4abec7a94ff"})}, :xt/id #uuid "bac33e5a-da23-41ad-9a75-aa50c0e9b35d"}))


(defn movement-results->prs
  [movement-results]
  (->> movement-results
       (map :result/type)
       (map (fn [mr]
              (into {} (map (fn [{:result-set/keys [reps weight status]}]
                              [reps (if (= :pass status)
                                      weight
                                      0)]) (:result-set/_parent mr)))))
       (apply merge-with max)))


(defn round
  "Rounds 'x' to 'places' decimal places"
  [places, x]
  (->> x
       bigdec
       (#(.setScale % places java.math.RoundingMode/HALF_UP))
       .doubleValue))


(def percentages
  (partition 4 (map (partial round 2) (range 1.05 0.25 -0.05))))


(defn tabs
  [{:keys [id tabs]}]
  [:nav.isolate.flex.divide-x.divide-black.shadow.border-black.border-t-2.border-r-2.border-l-2
   {:aria-label "Tabs"
    :id         id}
   (map (fn [{:keys [name options]}]
          [:a
           (merge {:class (concat '[group relative min-w-0 flex-1 overflow-hidden bg-white py-4 px-4 text-center text-sm font-medium hover:bg-gray-50 focus:z-10]
                                  (if (:selected options)
                                    '[text-gray-900]
                                    '[text-gray-500 hover:text-gray-700]))} options)
           [:span
            {:class       (concat '[]
                                  (if (:selected options)
                                    '[font-bold]
                                    '[]))
             :aria-hidden "true"}
            name]
           [:span
            {:class       (concat '[absolute inset-x-0 bottom-0 "h-[2px]"]
                                  (if (:selected options)
                                    '[bg-black]
                                    '[bg-transparent]))
             :aria-hidden "true"}]]) tabs)])


(defn percentage-ui
  [n]
  [:ul.flex.flex-col.justify-center.items-center.gap-2.bg-white.md:p-8.p-5.border-black.border-2
   {:id "percentages"}
   (map (fn [ps]
          [:li.flex.flex-row.justify-between.w-full
           (map (fn [p]
                  [:div.flex.flex-col.gap-1.text-center
                   [:p.m-0.leading-normal.text-lg.md:text-2xl.font-bold
                    (if n
                      (int (round 1 (* p n)))
                      "--")]
                   [:p.m-0.leading-normal.text-sm.md:text-md.text-gray-500.uppercase (str (int (* 100 p)) "%")]]) ps)])
        percentages)])


(defn tabs-percentage
  [{:keys [biff/db params session path-params] :as _ctx}]
  (let [movement-id      (parse-uuid (:id path-params))
        movement-results (biff/q db '{:find  (pull result [* {:result/type
                                                              [*
                                                               {:result-set/_parent [*]}]}])
                                      :in    [[user movement]]
                                      :where [[result :result/user user]
                                              [result :result/type type]
                                              [result :result/date date]
                                              [type :result/movement movement]]}
                                 [(:uid session) movement-id])
        prs              (movement-results->prs movement-results)
        current-rep      (parse-int (:id params))
        reps             [1 2 3 5]]
    [:<>
     (tabs {:id   "reps-tab"
            :tabs   (map (fn [r]
                           {:name    (str r " Rep")
                            :options (if (= current-rep r)
                                       {:selected     true
                                        :aria-current "page"}
                                       {:hx-get        (str "/app/movements/" movement-id "/tabs?id=" r "&weight=" (get prs r))
                                        :hx-select     "#reps-tab"
                                        :hx-target     "#reps-tab"
                                        :hx-swap       "outerHTML"
                                        :hx-select-oob "#percentages"})}) reps)})
     (percentage-ui (safe-parse-int (:weight params)))]))


(defn show
  [{:keys [biff/db path-params session] :as ctx}]
  (let [movement-id      (parse-uuid (:id path-params))
        m                (xt/entity db movement-id)
        movement-results (map second (biff/q db '{:find     [date (pull result [* {:result/type
                                                                                   [*
                                                                                    {:result-set/_parent [*]}]}])]
                                                  :in       [[user movement]]
                                                  :where    [[result :result/user user]
                                                             [result :result/type type]
                                                             [result :result/date date]
                                                             [type :result/movement movement]]
                                                  :order-by [[date :desc]]}
                                             [(:uid session) movement-id]))
        workouts         (biff/q db '{:find  (pull w [*])
                                      :in    [[movement user]]
                                      :where [[w :workout/name]
                                              (or [w :workout/user  user]
                                                  (and [w :workout/name]
                                                       (not [w :workout/user])))
                                              [wm :workout-movement/workout w]
                                              [wm :workout-movement/movement movement]]}
                                 [movement-id (:uid session)])
        prs              (movement-results->prs movement-results)]
    (ui/page ctx (ui/panel
                   [:div
                    [:div.flex.justify-between.items-center.mb-7
                     [:h1.text-3xl.cursor-default.capitalize (:movement/name m)]
                     [:a.btn.bg-brand-teal {:href (str "/app/movements/" (:xt/id m) "/new")} "Log Lift"]]
                    (when (empty? movement-results)
                      [:div
                       [:p.text-md "Log a session and it will show up here."]])
                    (when (not-empty movement-results)
                      [:<>
                       [:div.mb-7.flex.flex-col.gap-7.md:flex-row.w-full
                        [:div.flex-1
                         [:h2.text-lg.leading-7.text-gray-900.sm:truncate.sm:text-lg.sm:tracking-tight.mb-2
                          "Personal Records"]
                         [:div.bg-white.md:p-6.p-2.border-black.border-2
                          [:ul.list-none.flex.md:justify-around.justify-between.align-center
                           (map (fn [n]
                                  [:li
                                   [:div.flex.flex-col.justify-center.items-center.gap-2
                                    [:p.m-0.text-lg.md:text-2xl.font-bold (or (get prs n) "--")]
                                    [:p.m-0.text-sm.md:text-md.text-gray-500.uppercase (str n " Rep Max")]]])
                                [1 2 3 5])]]]
                        [:div.flex-1
                         [:h2.text-lg.leading-7.text-gray-900.sm:truncate.sm:text-lg.sm:tracking-tight.mb-2
                          "Percentages"]
                         [:div.max-w-lg
                          (tabs {:id   "reps-tab"
                                 :tabs [{:name    "1 Rep"
                                         :options {:selected true
                                                   :aria-current "page"}}
                                        {:name          "2 Rep"
                                         :options       {:hx-get (str "/app/movements/" movement-id "/tabs?id=2" "&weight=" (get prs 2))
                                                         :hx-select "#reps-tab"
                                                         :hx-target "#reps-tab"
                                                         :hx-swap "outerHTML"
                                                         :hx-select-oob "#percentages"}}
                                        {:name          "3 Rep"
                                         :options       {:hx-get (str "/app/movements/" movement-id "/tabs?id=3"  "&weight=" (get prs 3))
                                                         :hx-select "#reps-tab"
                                                         :hx-target "#reps-tab"
                                                         :hx-swap "outerHTML"
                                                         :hx-select-oob "#percentages"}}
                                        {:name          "5 Rep"
                                         :options       {:hx-get (str "/app/movements/" movement-id "/tabs?id=5"  "&weight=" (get prs 5))
                                                         :hx-select "#reps-tab"
                                                         :hx-target "#reps-tab"
                                                         :hx-swap "outerHTML"
                                                         :hx-select-oob "#percentages"}}]})
                          (percentage-ui (get prs 1))]]]
                       [:h2.text-lg.leading-7.text-gray-900.sm:truncate.sm:text-lg.sm:tracking-tight.mb-2
                        "History"]
                       [:div {:class "grid grid-cols-[1fr_1fr_1fr_minmax(30px,170px)] w-full border-b-4 border-r-4 border-black mb-4  bg-white"}
                        [:div.flex.justify-center.items-center.px-2.py-4.border-2.border-black.text-lg.font-bold "Date"]
                        [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-t-2.border-black.text-lg.font-bold.whitespace-nowrap "Rep Scheme"]
                        [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-t-2.border-black.text-lg.font-bold.whitespace-nowrap "Best Lift"]
                        [:div.border-r-2.border-b-2.border-t-2.border-black ""]
                        (map movement-results-ui movement-results)]])
                    (when (not-empty workouts)
                      [:div
                       [:h2.text-xl.mb-4 "Related workouts"]
                       [:div.flex.gap-2.sm:gap-4.flex-wrap.justify-center.pb-20
                        (map movement-workout-ui workouts)]])]))))


(defn get-constant-strength-sets
  [{:keys [n] :as opts}]
  (let [count (atom 0)]
    (repeatedly n
                #(strength-set-inputs (merge {:number (swap! count inc)} opts)))))


(defn get-variable-strength-sets
  [{:keys [sets reps] :as _opts}]

  (loop [set-n      0
         result [:div]]
    (if (<= sets set-n)
      result
      (recur (inc set-n)
             (conj result (strength-set-inputs {:number (inc set-n) :reps (nth reps set-n)}))))))


(defn params->reps
  [{:keys [reps type] :as _params}]
  (cond
    (= "constant" type) (safe-parse-int reps)
    (= "variable" type) (->> (string/split reps #",")
                             (mapv safe-parse-int))
    :else reps))


(defn create-log-session-form
  [{:keys [session path-params params] :as _ctx}]
  (let [reps        (params->reps params)
        sets        (safe-parse-int (:sets params))
        type        (:type params)
        strength-id (random-uuid)]
    (biff/form  {:hidden {:user     (:uid session)
                          :movement (:id path-params)
                          :strength strength-id
                          :reps     reps
                          :sets     sets}
                 :method "POST"
                 :action (str "/app/movements/" (:id path-params) "/set")}
                (when (= type "constant")
                  [:div (get-constant-strength-sets {:n sets :reps reps})])
                (when (= type "variable")
                  [:div (get-variable-strength-sets {:sets sets
                                                     :reps reps})])
                [:div.flex.flex-col.justify-center.items-center.gap-4
                 [:input.pink-input.teal-focus.mt-4.mx-auto
                  {:type  "date"
                   :name  "date"
                   :value (biff/format-date
                            (biff/now) "YYYY-MM-dd")}]
                 [:textarea#notes
                  {:name        "notes"
                   :placeholder "notes"
                   :rows        7
                   :class       (str "w-full pink-input teal-focus")}]
                 [:button.btn "Submit"]])))


(defn variable-reps-form
  [{:keys [path-params] :as _ctx}]
  (biff/form {:id      "sets-scheme"
              :hx-get  (str "/app/movements/" (:id path-params) "/form")
              :hx-swap "outerHTML"}
             [:select.bg-brand-background.brutal-shadow.teal-focus.cursor-pointer.block.mx-auto.sm:mx-0
              {:name      :type
               :hx-get    (str "/app/movements/" (:id path-params) "/constant-reps")
               :hx-target "#sets-scheme"
               :hx-swap   "outerHTML"}
              [:option {:value :constant} "Constant Reps"]
              [:option {:value    :variable
                        :selected true} "Variable Reps"]]
             [:div.mt-8.mx-auto {:x-data "{reps: [5, 5, 5, 5, 5]}"}
              [:div.flex.flex-row.justify-around.gap-8.md:mx-20
               [:div.w-36.text-center
                [:label.text-2xl.font-bold.block.text-center.mb-2 {:for :sets} "Sets"]
                [:p.text-9xl.font-bold.cursor-default {:x-text "reps.length"}]
                [:input {:name    :sets
                         :id      :sets
                         :type    :hidden
                         ":value" "reps.length"}]
                [:div.space-x-2
                 [:button.btn.w-12 {:x-on:click "if (reps.length > 1) reps.pop()"
                                    :type       "button"} "-"]
                 [:button.btn.w-12 {:x-on:click "reps.push(5)"
                                    :type       "button"} "+"]]]
               [:input#reps {":value" "reps.join(',')"
                             :name    :reps
                             :type    :hidden}]
               [:.flex.gap-2.self-center.m-0.flex-col
                [:template {:x-for "(rep, index) in reps"}
                 [:div.text-center {:x-id "['rep']"}
                  [:div.flex.flex-row.gap-4
                   [:p {:class (str "w-4 text-xl font-bold cursor-default opacity-30 self-center")
                        :x-text "index + 1"}]
                   [:p.text-3xl.font-bold.cursor-default.w-12 {:x-text "rep"}]
                   [:label.text-2xl.font-bold.block.text-center.mb-2 {":for" "$id('rep')"} "Reps"]
                   [:div.gap-2.flex.flex-row
                    [:button {:x-on:click "if(reps[index] > 1) reps[index]--"
                              :type       "button"
                              :class      (str " border-2 border-black font-bold text-black shadow-[2px_2px_0px_rgba(0,0,0,100)] h-8 w-8 text-center bg-brand-background ")} "-"]
                    [:button {:x-on:click "reps[index]++"
                              :type       "button"
                              :class (str " border-2 border-black font-bold text-black shadow-[2px_2px_0px_rgba(0,0,0,100)] h-8 w-8 text-center bg-brand-background ")} "+"]]]]]]]]
             [:button.btn.mt-8.block.mx-auto {:type "submit"} "Submit"]))


(defn constant-reps-form
  [{:keys [path-params] :as _ctx}]
  (biff/form {:id      "sets-scheme"
              :hx-get  (str "/app/movements/" (:id path-params) "/form")
              :hx-swap "outerHTML"}
             [:select.bg-white.brutal-shadow.teal-focus.cursor-pointer.block.mx-auto.sm:mx-0
              {:name      :type
               :hx-get    (str "/app/movements/" (:id path-params) "/variable-reps")
               :hx-target "#sets-scheme"
               :hx-swap   "outerHTML"}
              [:option {:value    :constant
                        :selected true} "Constant Reps"]
              [:option {:value :variable} "Variable Reps"]]
             [:div.w-fit.mt-8.mx-auto {:x-data "{sets: 5, reps: 5}"}
              [:div.flex.flex-row.justify-center.gap-8
               [:div.w-36.text-center
                [:label.text-2xl.font-bold.block.text-center.mb-2 {:for :sets} "Sets"]
                [:p.text-5xl.sm:text-9xl.font-bold.cursor-default {:x-text :sets}]
                [:input {:x-model :sets
                         :name    :sets
                         :id      :sets
                         :type    :hidden}]
                [:div.space-x-2
                 [:button.btn.w-12 {:x-on:click "if (sets > 1) sets--"
                                    :type       "button"} "-"]
                 [:button.btn.w-12 {:x-on:click "sets++"
                                    :type       "button"} "+"]]]
               [:p.text-3xl.sm:text-7xl.font-bold.self-center.cursor-default "X"]
               [:div.w-36.text-center
                [:label.text-2xl.font-bold.block.text-center.mb-2 {:for :reps} "Reps"]
                [:p.text-5xl.sm:text-9xl.font-bold.cursor-default {:x-text :reps}]
                [:input {:x-model :reps
                         :name    :reps
                         :id      :reps
                         :type    :hidden}]
                [:div.space-x-2

                 [:button.btn.w-12 {:x-on:click "if (reps > 1) reps--"
                                    :type       "button"} "-"]
                 [:button.btn.w-12 {:x-on:click "reps++"
                                    :type       "button"} "+"]]]]]
             [:button.btn.bg-brand-teal.mt-8.block.mx-auto {:type "submit"} "Submit"]))


(defn log-session
  [{:keys [biff/db path-params] :as ctx}]
  (let [movement-id (parse-uuid (:id path-params))
        m                (xt/entity db movement-id)]
    (ui/page ctx [:div {:class (str "lg:w-2/3 mx-auto pb-8")}
                  (ui/panel
                    [:div.flex.justify-around.sm:justify-between.my-4
                     [:h1.text-3xl.cursor-default.capitalize.self-center (:movement/name m)]
                     [:a.btn-no-bg.w-fit.self-center {:href (str "/app/movements/" (:id path-params))} "Back"]]
                    (constant-reps-form ctx))])))


(defn ->sets-tx
  [{:keys [sets] :as params} parent]
  (mapv (fn [n]
          (let [set-n       (inc n)
                key-builder (partial ->key set-n)
                reps-key    (key-builder "reps")
                status-key  (key-builder "hit-miss")
                weight-key  (key-builder "weight")]
            {:db/doc-type       :strength-set
             :result-set/parent parent
             :result-set/reps   (parse-int (reps-key params))
             :result-set/status (if (= "hit" (status-key params))
                                  :pass
                                  :fail)
             :result-set/number set-n
             :result-set/weight (parse-int (weight-key params))}))
        (range (parse-int sets))))


(defn create-session
  [{:keys [path-params session params] :as ctx}]
  (let [tx (concat [{:xt/id            :db.id/strength-result
                     :db/doc-type      :strength-result
                     :db/op            :create
                     :result/movement  (parse-uuid (:movement params))
                     :result/notes     (:notes params)
                     :result/set-count (parse-int (:sets params))}
                    {:xt/id       :db.id/result
                     :db/doc-type :result
                     :db/op       :create
                     :result/date (instant/read-instant-date (:date params))
                     :result/user (:uid session)
                     :result/type :db.id/strength-result}]
                   (->sets-tx params :db.id/strength-result))]
    (biff/submit-tx ctx tx)
    {:status  303
     :headers {"Location" (str "/app/movements/" (:id path-params))}}))


(defn ->update-tx
  [{:keys [sets] :as params}]
  (mapv (fn [n]
          (let [set-n       (inc n)
                key-builder (partial ->key set-n)
                id          (key-builder "id")
                status-key  (key-builder "hit-miss")
                weight-key  (key-builder "weight")]
            {:db/doc-type       :strength-set
             :db/op             :update
             :xt/id             (parse-uuid (id params))
             :result-set/status (if (= "hit" (status-key params))
                                  :pass
                                  :fail)
             :result-set/weight (parse-int (weight-key params))}))
        (range (parse-int sets))))


(defn update-result-set
  [{:keys [path-params params] :as ctx}]
  (let [result-tx (if (:date params)
                    [{:xt/id       (parse-uuid (:result-id path-params))
                      :db/doc-type :result
                      :db/op       :update
                      :result/date (instant/read-instant-date (:date params))}]
                    [])
        type-tx   (if (and (:notes params) (:strength-result-id params))
                    [{:xt/id        (parse-uuid (:strength-result-id params))
                      :db/doc-type  :strength-result
                      :db/op        :update
                      :result/notes (:notes params)}]
                    [])
        sets-tx   (->update-tx params)
        location  (or (:location params) (str "/app/movements/" (:id path-params) "/results/" (:result-id path-params)))]
    (biff/submit-tx ctx (concat result-tx type-tx sets-tx))
    {:status  303
     :headers {"Location" location}}))


(def routes
  ["/movements"
   ["/" {:get index
         :post create}]
   ["/:id"
    ["" {:get (wildcard-override show {:search search
                                       :new new})}]
    ["/constant-reps" {:get constant-reps-form}]
    ["/variable-reps" {:get variable-reps-form}]
    ["/tabs" {:get tabs-percentage}]
    ["/set" {:post create-session}]
    ["/form" {:get create-log-session-form}]
    ["/new" {:get log-session}]
    ["/results/:result-id/edit" {:get expanded-edit-result}]
    ["/results/:result-id" {:get    expanded-result
                            :delete closed-result
                            :put    update-result-set}]]])
