(ns com.spicy.results.core
  (:require
    [clojure.instant :as instant]
    [com.biffweb :as biff]
    [com.spicy.middleware :as mid]
    [com.spicy.movements.ui :refer [strength-set-inputs]]
    [com.spicy.results.score :refer [scores->tx ->scores]]
    [com.spicy.results.ui :refer [result-ui result-form normalized-result]]
    [com.spicy.route-helpers :refer [wildcard-override]]
    [com.spicy.ui :as ui]
    [xtdb.api :as xt]))


(defn show
  [{:keys [biff/db session path-params]}]
  (let [result (first (biff/q db '{:find (pull result [* {:result/type [*
                                                                        {:result/workout [*]}
                                                                        {:result/movement [*]}
                                                                        {:result-set/_parent [*]}]}])
                                   :in [[result-id user]]
                                   :where [[result :xt/id result-id]
                                           [result :result/user user]]}
                              [(parse-uuid (:id path-params)) (:uid session)]))]
    (result-ui result)))


(defn update-handler
  [{:keys [biff/db params session path-params] :as ctx}]
  (let [result      (first (biff/q db '{:find  (pull result [* {:result/type [*
                                                                              {:result/workout [*]}
                                                                              {:result-set/_parent [*]}]}])
                                        :in    [[result-id user]]
                                        :where [[result :xt/id result-id]
                                                [result :result/user user]]}
                                   [(parse-uuid (:id path-params)) (:uid session)]))
        workout     (-> result :result/type :result/workout)
        wod-sets-tx (transduce
                      (scores->tx {:op :update :parent (-> result :result/type :xt/id)})
                      conj
                      (->scores
                        (merge params
                               {:reps-per-round  (:workout/reps-per-round workout)
                                :scheme          (:workout/scheme workout)
                                :rounds-to-score (:workout/rounds-to-score workout)})))]
    (biff/submit-tx ctx
                    (concat
                      [{:xt/id       (parse-uuid (:id path-params))
                        :db/op       :update
                        :db/doc-type :result
                        :result/date (instant/read-instant-date (:date params))}
                       {:db/op        :update
                        :db/doc-type  :wod-result
                        :xt/id        (-> result :result/type :xt/id)
                        :result/notes (:notes params)
                        :result/scale (keyword (:scale params))}]
                      wod-sets-tx))
    {:status  303
     :headers {"Location" (str "/app/results/" (:id path-params))}}))


(defn remove-keys-ns
  [m]
  (update-keys m (comp keyword name)))


(defn edit
  [{:keys [biff/db session path-params] :as _ctx}]
  (let [{:result/keys [type] :as result}
        (first (biff/q db '{:find  (pull result [*
                                                 {:result/type [*
                                                                {:result/movement [*]}
                                                                {:result/workout [*]}
                                                                {:result-set/_parent [*]}]}])
                            :in    [[result-id user]]
                            :where [[result :xt/id result-id]
                                    [result :result/user user]]}
                       [(parse-uuid (:id path-params)) (:uid session)]))
        result-path (str "/app/results/" (:xt/id result))
        workout?    (seq (:result/workout type))
        movement?   (seq (:result/movement type))]
    [:div#edit-result
     [:h2.text-2xl.font-bold (:name (normalized-result result))]
     (when movement?
       (biff/form  {:hidden    {:sets      (count (-> result :result/type :result-set/_parent))
                                :strength-result-id   (:xt/id type)
                                :location  result-path}
                    :hx-put    (str "/app/movements/" (-> result :result/type :result/movement :xt/id) "/results/" (:xt/id result))
                    :hx-target "closest #edit-result"}
                   (map (comp strength-set-inputs remove-keys-ns) (sort-by :result-set/number (:result-set/_parent type)))
                   [:div.flex.flex-col.justify-center.items-center.gap-4
                    [:input.pink-input.teal-focus.mt-4.mx-auto
                     {:type  "date"
                      :name  "date"
                      :value (biff/format-date
                               (or (:result/date result) (biff/now)) "YYYY-MM-dd")}]
                    [:textarea#notes
                     {:name        "notes"
                      :placeholder "notes"
                      :value       (:result/notes type)
                      :rows        7
                      :class       (str "w-full pink-input teal-focus")}]
                    [:button.btn.w-full {:type "submit"} "Update Result"]
                    [:button.btn.bg-red-400.hover:bg-red-600.w-full {:hx-get    result-path
                                                                     :hx-target "closest #edit-result"} "Cancel"]]))
     (when workout?
       (result-form
         (assoc {}
                :result result
                :workout (:result/workout type)
                :action result-path
                :hx-key :hx-put
                :form-props {:hx-target "closest #edit-result"
                             :hx-swap   "outerHTML"})
         [:button.btn {:type "submit"} "Update Result"]
         [:button.btn.bg-red-400.hover:bg-red-600 {:hx-get    result-path
                                                   :hx-target "closest #edit-result"} "Cancel"]))]))


(defn index
  [{:keys [biff/db session] :as ctx}]
  (let [date-and-results (biff/q db '{:find     [date (pull result
                                                            [*
                                                             {:result/type [*
                                                                            {:result/workout [*]}
                                                                            {:result/movement [*]}
                                                                            {:result-set/_parent [*]}]}])]
                                      :in       [[user]]
                                      :where    [[result :result/user user]
                                                 [result :result/date date]]
                                      :order-by [[date :desc]]}
                                 [(:uid session)])
        results (map second date-and-results)]
    (ui/page ctx (ui/panel [:div {:class (str "p-4 max-w-xl sm:mx-auto")}
                            [:h1.text-3xl.cursor-default.capitalize.text-center.sm:text-left "Results"]
                            [:ul.list-none.p-0.m-0.mt-4.space-y-4
                             (map (fn [r]
                                    [:li.p-4.border-2.border-black.bg-white
                                     (result-ui r)]) results)]]))))


(defn create
  [{:keys [biff/db session params] :as ctx}]
  (let [workout-id                       (parse-uuid (:workout params))
        {:workout/keys
         [reps-per-round
          scheme
          rounds-to-score] :as _workout} (xt/entity db workout-id)
        result-tx                        [{:db/op          :create
                                           :db/doc-type    :wod-result
                                           :xt/id          :db.id/wod-result
                                           :result/workout workout-id
                                           :result/scale   (keyword (:scale params))
                                           :result/notes   (:notes params)}
                                          {:db/op       :merge
                                           :db/doc-type :result
                                           :result/user (:uid session)
                                           :result/date (instant/read-instant-date (:date params))
                                           :result/type :db.id/wod-result}]
        wod-sets-tx                      (transduce
                                           (scores->tx {:op :create :parent :db.id/wod-result})
                                           conj
                                           (->scores
                                             (merge params
                                                    {:reps-per-round  reps-per-round
                                                     :scheme          scheme
                                                     :rounds-to-score rounds-to-score})))]

    (biff/submit-tx ctx (concat result-tx wod-sets-tx)))
  (let [{:xt/keys [id] :as w} (xt/entity db (parse-uuid (:workout params)))]
    {:status  303
     :headers {"location" (str "/app/workouts/" id)}}))


(defn new
  [{:keys [biff/db params] :as ctx}]
  (let [workout (first (biff/q db
                               '{:find (pull workout [*])
                                 :in [[id]]
                                 :where [[workout :xt/id id]]}
                               [(parse-uuid (:workout params))]))]

    (ui/page ctx [:div.pb-8
                  (ui/panel
                    [:div {:class (str "md:min-h-[60vh] p-4 pb-8")}
                     [:h1 {:class (str "text-5xl mt-8 mb-14 text-center sm:text-left")} "Log Result"]
                     [:.flex.flex-col.gap-6.md:flex-row
                      (ui/workout-ui workout)
                      [:.flex-1
                       (result-form
                         (merge {:workout workout} {:action "/app/results" :hidden {:workout (:xt/id workout)}})
                         [:button.btn {:type "submit"} "Log Result"])]]])])))


(def routes
  ["/results"
   ["" {:get  index
        :post create}]
   ["/:id" {:middleware [(partial mid/wrap-ensure-owner #{"new"})]}
    ["" {:get (wildcard-override show {:new new})
         :put update-handler}]
    ["/edit" {:get edit}]]])
