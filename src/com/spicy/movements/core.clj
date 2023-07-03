 (ns com.spicy.movements.core
   (:require
     [clojure.instant :as instant]
     [clojure.string :as string]
     [com.biffweb :as biff]
     [com.spicy.ui :as ui]
     [xtdb.api :as xt]))


(defn ->key
  [n s]
  (keyword (str s "-" n)))


(defn htmx-request?
  [ctx]
  (-> ctx :headers
      (get "hx-request")
      (= "true")))


(defn movements-list
  [movements]
  [:ul#movement-list.list-none.flex.flex-wrap.gap-4.mt-8.justify-center
   (map (fn [m]
          [:li {:class (str "w-fit z-[1] rounded border-2 border-black bg-brand-background p-2 text-2xl font-bold text-black text-center whitespace-nowrap hover:border-brand-teal hover:shadow-[2px_2px_0px_rgba(131, 242, 179,100)")}
           [:a {:href (str "/app/movements/" (:xt/id m))} (:movement/name m)]]) movements)])


(defn movement-workout-ui
  [w]
  (ui/workout-ui (assoc w
                        :children
                        [:a.brutal-shadow.btn-hover.border.border-radius.rounded.border-black.py-1.px-2.mt-auto
                         {:href (str "/app/workouts/" (string/lower-case
                                                        (if (:workout/user w)
                                                          (:xt/id w)
                                                          (:workout/name w))))}
                         "History"])))


(defn index
  [{:keys [biff/db params] :as ctx}]
  (let [movements (biff/q db '{:find  (pull m [*])
                               :in    [[type]]
                               :where [[m :movement/name]
                                       [m :movement/type type]]}
                          [(or (keyword (:type params)) :strength)])]
    (if (htmx-request? ctx)
      (movements-list movements)
      (ui/page {} (ui/panel [:div
                             [:div.flex.justify-between.mt-8
                              [:h1.text-5xl.w-fit.self-center "Movements"]
                              [:select.btn.text-base.w-32.h-12.teal-focus {:name      "type"
                                                                           :onchange "window.open('?type=' + this.value,'_self')"}
                               [:option.text-base {:value :strength :selected (or (= (:type params) "stregnth") (empty? (:type params)))} "Strength"]
                               [:option.text-base {:value :gymnastic :selected (= (:type params) "gymnastic")} "Gymnastic"]
                               [:option.text-base {:value :monostructural :selected (= (:type params) "monostructural")} "Cardio"]]]
                             (movements-list movements)])))))


(defn sets-n-reps
  [sets]
  (let [reps (into #{} (map :result-set/reps sets))]
    (if (= 1 (count reps))
      (string/join (interpose "x" [(count sets) (count reps)]))
      (string/join (interpose "-" (map :result-set/reps  (sort-by :result-set/number sets)))))))


(defn movement-results-ui
  [{:result/keys [type] :as result}]
  (let [sets (:result-set/_parent type)]
    [:div.flex.gap-2.sm:gap-4.flex-wrap.justify-center
     [:p.m-0 (sets-n-reps sets)]
     [:p.m-0 (apply max (map #(if (= :pass (:result-set/status %))
                                (:result-set/weight %)
                                0) sets))]]))


(defn show
  [{:keys [biff/db path-params session]}]
  (let [movement-id (parse-uuid (:id path-params))
        m                (xt/entity db movement-id)
        movement-results (biff/q db '{:find  (pull result [* {:result/type
                                                              [*
                                                               {:result-set/_parent [*]}]}])
                                      :in    [[user movement]]
                                      :where [[result :result/user user]
                                              [result :result/type type]
                                              [type :result/movement movement]]}
                                 [(:uid session) movement-id])
        workouts         (biff/q db '{:find  (pull w [*])
                                      :in    [[movement user]]
                                      :where [[w :workout/name]
                                              (or [w :workout/user  user]
                                                  (and [w :workout/name]
                                                       (not [w :workout/user])))
                                              [wm :workout-movement/workout w]
                                              [wm :workout-movement/movement movement]]}
                                 [movement-id (:uid session)])]
    (ui/page {} (ui/panel
                  [:div
                   [:div.flex.justify-between.items-center.mb-14
                    [:h1.text-3xl.cursor-default.capitalize (:movement/name m)]
                    [:a.btn {:href (str "/app/movements/" (:xt/id m) "/new")} "Log session"]]
                   (when (not-empty movement-results)
                     (map movement-results-ui movement-results))
                   (when (not-empty workouts)
                     [:div
                      [:h2.text-xl.mb-4 "Related workouts"]
                      [:div.flex.gap-2.sm:gap-4.flex-wrap.justify-center.pb-20
                       (map movement-workout-ui workouts)]])]))))


(defn parse-int
  [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))


(defn safe-parse-int
  [s]
  (try
    (parse-int s)
    (catch Exception e
      (prn "Error while parsing int: " e))))


(defn strength-set-inputs
  [{:keys [set-number reps]}]
  [:div
   [:p (str "Set #" set-number)]
   [:input {:value reps
            :type  "hidden"
            :id    (str "reps-" set-number)
            :name  (str "reps-" set-number)}]
   [:.flex.gap-2.items-center.m-0
    [:input {:name     (str "weight-" set-number)
             :id       (str "weight-" set-number)
             :required true
             :type     :number}]
    [:p.m-0 (str "x " reps " reps")]]
   [:fieldset
    [:.flex.gap-2
     [:label
      [:input {:name  (str "hit-miss-" set-number)
               :id    (str "hit-" set-number)
               :value :hit
               :type  :checkbox}]
      "Hit"]]]])


(defn get-constant-strength-sets
  [{:keys [n] :as opts}]
  (let [count (atom 0)]
    (repeatedly n
                #(strength-set-inputs (merge {:set-number (swap! count inc)} opts)))))


(defn get-variable-strength-sets
  [{:keys [sets] :as opts}]
  (loop [set-n      1
         result [:div]]
    (if (< sets set-n)
      result
      (recur (inc set-n)
             (conj result (strength-set-inputs {:set-number set-n :reps (get opts (->key set-n "rep"))}))))))


(defn create-log-session-form
  [{:keys [session path-params params] :as ctx}]
  (let [reps        (safe-parse-int (:reps params))
        sets        (safe-parse-int (:sets params))
        type        (:type params)
        strength-id (random-uuid)]
    (tap> ctx)
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
                 [:div (get-variable-strength-sets (merge params {:sets sets}))])
               [:input.pink-input.teal-focus
                {:type  "date"
                 :name  "date"
                 :value (biff/format-date
                          (biff/now) "YYYY-MM-dd")}]
               [:textarea.w-full.pink-input.teal-focus#notes
                {:name        "notes"
                 :placeholder "notes"}]
               [:button.btn "Submit"])))


(defn variable-reps-form
  [{:keys [path-params] :as _ctx}]
  (biff/form {:id      "sets-scheme"
              :hx-get  (str "/app/movements/" (:id path-params) "/form")
              :hx-swap "outerHTML"}
             [:select.rounded.bg-brand-background.brutal-shadow.teal-focus.cursor-pointer.block.mx-auto.sm:mx-0
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
                              :class      (str " border-2 border-black rounded font-bold text-black shadow-[2px_2px_0px_rgba(0,0,0,100)] h-8 w-8 text-center bg-brand-background ")
                              }"-"]
                    [:button {:x-on:click "reps[index]++" 
                              :type       "button"
                              :class (str " border-2 border-black rounded font-bold text-black shadow-[2px_2px_0px_rgba(0,0,0,100)] h-8 w-8 text-center bg-brand-background ")} "+"]]
                   ]
                  
                  [:input {:x-model "reps[index]"
                           ":name"  "$id('rep')"
                           ":id"    "$id('rep')"
                           :type    :hidden}]
                  ]]]]]
             [:button.btn.mt-8.block.mx-auto {:type "submit"} "Submit"]))


(defn constant-reps-form
  [{:keys [path-params] :as _ctx}]
  (biff/form {:id      "sets-scheme"
              :hx-get  (str "/app/movements/" (:id path-params) "/form")
              :hx-swap "outerHTML"}
             [:select.rounded.bg-brand-background.brutal-shadow.teal-focus.cursor-pointer.block.mx-auto.sm:mx-0
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
                [:p.text-9xl.font-bold.cursor-default {:x-text :sets}]
                [:input {:x-model :sets
                         :name    :sets
                         :id      :sets
                         :type    :hidden}]
                [:div.space-x-2
                 [:button.btn.w-12 {:x-on:click "if (sets > 1) sets--"
                                    :type       "button"} "-"]
                 [:button.btn.w-12 {:x-on:click "sets++"
                                    :type       "button"} "+"]]]
               [:p.text-7xl.font-bold.self-center.cursor-default "X"]
               [:div.w-36.text-center
                [:label.text-2xl.font-bold.block.text-center.mb-2 {:for :reps} "Reps"]
                [:p.text-9xl.font-bold.cursor-default {:x-text :reps}]
                [:input {:x-model :reps
                         :name    :reps
                         :id      :reps
                         :type    :hidden}]
                [:div.space-x-2

                 [:button.btn.w-12 {:x-on:click "if (reps > 1) reps--"
                                    :type       "button"} "-"]
                 [:button.btn.w-12 {:x-on:click "reps++"
                                    :type       "button"} "+"]]]]]
             [:button.btn.mt-8.block.mx-auto {:type "submit"} "Submit"]))


(defn log-session
  [{:keys [biff/db path-params] :as ctx}]
  (let [movement-id (parse-uuid (:id path-params))
        m                (xt/entity db movement-id)]
    (ui/page {} [:div {:class (str "lg:w-2/3 mx-auto pb-8")}
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
  (tap> params)
  (let [result-id (random-uuid)
        tx (concat [{:xt/id            :db.id/strength-result
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


(def routes
  ["/movements"
   ["/" {:get index}]
   ["/:id"
    ["" {:get show}]
    ["/constant-reps" {:get constant-reps-form}]
    ["/variable-reps" {:get variable-reps-form}]
    ["/set" {:post create-session}]
    ["/form" {:get create-log-session-form}]
    ["/new" {:get log-session}]]])
