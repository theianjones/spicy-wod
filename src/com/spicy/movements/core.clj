 (ns com.spicy.movements.core
   (:require
     [clojure.instant :as instant]
     [clojure.string :as string]
     [com.biffweb :as biff]
     [com.spicy.numbers :refer [parse-int safe-parse-int]]
     [com.spicy.route-helpers :refer [wildcard-override]]
     [com.spicy.route-helpers :refer [->key htmx-request?]]
     [com.spicy.movements.ui :refer [movement-form]]
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
              (movement-form {})
              ])]))

(defn create
  [{:keys [params] :as ctx}]
  (let [movement-uuid (random-uuid)
        new-movement  [{:db/op         :create
                        :db/doc-type   :movement
                        :movement/name (:name params)
                        :movement/type (keyword (:type params))
                        :xt/id         movement-uuid}]
        ]
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
       [:button.p-2.text-md.grow.bg-darker-brand-teal.sm:border-r-2.border-b-2.border-black.sm:border-b-0

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
                       :class "grid grid-cols-[1fr_1fr_1fr_minmax(30px,170px)] w-full bg-darker-brand-teal"}
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


(defn show
  [{:keys [biff/db path-params session] :as ctx}]
  (let [movement-id (parse-uuid (:id path-params))
        m                (xt/entity db movement-id)
        movement-results (map second (biff/q db '{:find  [date (pull result [* {:result/type
                                                                                [*
                                                                                 {:result-set/_parent [*]}]}])]
                                                  :in    [[user movement]]
                                                  :where [[result :result/user user]
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
                                 [movement-id (:uid session)])]
    (ui/page ctx (ui/panel
                   [:div
                    [:div.flex.justify-between.items-center.mb-14
                     [:h1.text-3xl.cursor-default.capitalize (:movement/name m)]
                     [:a.btn.bg-brand-teal {:href (str "/app/movements/" (:xt/id m) "/new")} "Log session"]]
                    (when (empty? movement-results)
                      [:div
                       [:p.text-md "Log a session and it will show up here."]])
                    (when (not-empty movement-results)
                      [:div {:class "grid grid-cols-[1fr_1fr_1fr_minmax(30px,170px)] w-full border-b-4 border-r-4 border-black mb-4 rounded-md bg-brand-teal"}
                       [:div.flex.justify-center.items-center.px-2.py-4.border-2.border-black.text-lg.font-bold "Date"]
                       [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-t-2.border-black.text-lg.font-bold.whitespace-nowrap "Rep Scheme"]
                       [:div.flex.justify-center.items-center.px-2.py-4.border-r-2.border-b-2.border-t-2.border-black.text-lg.font-bold.whitespace-nowrap "Best Lift"]
                       [:div.border-r-2.border-b-2.border-t-2.border-black ""]
                       (map movement-results-ui movement-results)])
                    (when (not-empty workouts)
                      [:div
                       [:h2.text-xl.mb-4 "Related workouts"]
                       [:div.flex.gap-2.sm:gap-4.flex-wrap.justify-center.pb-20
                        (map movement-workout-ui workouts)]])]))))


(defn strength-set-inputs
  [{:keys [set-number reps]}]
  [:div
   [:p.text-2xl.font-medium.text-center.mt-4.whitespace-nowrap (str "Set #" set-number)]
   [:input {:value reps
            :type  "hidden"
            :id    (str "reps-" set-number)
            :name  (str "reps-" set-number)}]
   [:.flex.justify-center.items-center.m-0
    [:fieldset
     [:.flex.gap-2
      [:label.text-lg.sm:text-2xl.font-medium.text-center.h-fit.my-auto
       {:for (str "hit-" set-number)}
       "Hit"]
      [:input {:name  (str "hit-miss-" set-number)
               :id    (str "hit-" set-number)
               :class (str "appearance-none p-7 border-2 border-r-0 border-black cursor-pointer "
                           "checked:bg-brand-teal checked:text-brand-teal checked:color-brand-teal hover:bg-brand-teal checked:border-black checked:ring-0 checked:ring-offset-0 checked:ring-brand-teal checked:ring-opacity-100 focus:outline-none focus:ring-0 focus:ring-offset-0 focus:ring-opacity-100")
               :value :hit
               :type  :checkbox}]]]
    [:input {:name     (str "weight-" set-number)
             :id       (str "weight-" set-number)
             :class    (str "p-4 border-2 border-black w-1/2 text-center font-bold teal-focus ")
             :required true
             :type     :number}]
    [:p.m-0.bg-white.p-4.border-2.border-l-0.border-black.font-medium.whitespace-nowrap (str "x " reps " reps")]]])


(defn get-constant-strength-sets
  [{:keys [n] :as opts}]
  (let [count (atom 0)]
    (repeatedly n
                #(strength-set-inputs (merge {:set-number (swap! count inc)} opts)))))


(defn get-variable-strength-sets
  [{:keys [sets reps] :as _opts}]

  (loop [set-n      0
         result [:div]]
    (if (<= sets set-n)
      result
      (recur (inc set-n)
             (conj result (strength-set-inputs {:set-number (inc set-n) :reps (nth reps set-n)}))))))


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
    (biff/form {:hidden {:user     (:uid session)
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
  (let [tx (->update-tx params)]
    (biff/submit-tx ctx tx)
    {:status  303
     :headers {"Location" (str "/app/movements/" (:id path-params) "/results/" (:result-id path-params))}}))


(def routes
  ["/movements"
   ["/" {:get index
        :post create}]
   ["/:id" 
    ["" {:get (wildcard-override show {:search search
                                       :new new})}]
    ["/constant-reps" {:get constant-reps-form}]
    ["/variable-reps" {:get variable-reps-form}]
    ["/set" {:post create-session}]
    ["/form" {:get create-log-session-form}]
    ["/new" {:get log-session}]
    ["/results/:result-id/edit" {:get expanded-edit-result}]
    ["/results/:result-id" {:get    expanded-result
                            :delete closed-result
                            :put    update-result-set}]]])
