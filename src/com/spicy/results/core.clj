(ns com.spicy.results.core
  (:require
    [clojure.instant :as instant]
    [com.biffweb :as biff]
    [com.spicy.results.ui :refer [result-ui result-form params->score]]
    [com.spicy.route-helpers :refer [wildcard-override]]
    [com.spicy.ui :as ui]
    [xtdb.api :as xt]))


(defn show
  [{:keys [biff/db session path-params]}]
  (let [result (first (biff/q db '{:find (pull result [* {:result/type [* {:result/workout [*]}]}])
                                   :in [[result-id user]]
                                   :where [[result :xt/id result-id]
                                           [result :result/user user]]}
                              [(parse-uuid (:id path-params)) (:uid session)]))]
    (result-ui result)))


(defn update-handler
  [{:keys [biff/db params session path-params] :as ctx}]

  (let [result      (first (biff/q db '{:find  (pull result [* {:result/workout [*]
                                                                :result/type    [* {:result-set/_parent [*]}]}])
                                        :in    [[result-id user]]
                                        :where [[result :xt/id result-id]
                                                [result :result/user user]]}
                                   [(parse-uuid (:id path-params)) (:uid session)]))
        result-sets (-> result :result/type :result-set/_parent)
        result-set  (first result-sets)]
    (biff/submit-tx ctx
                    [{:xt/id       (parse-uuid (:id path-params))
                      :db/op       :update
                      :db/doc-type :result
                      :result/date (instant/read-instant-date (:date params))}
                     {:db/op        :update
                      :db/doc-type  :wod-result
                      :xt/id        (:result/type result)
                      :result/notes (:notes params)
                      :result/scale (keyword (:scale params))}
                     {:db/op            :update
                      :db/doc-type      :wod-set
                      :xt/id            (:xt/id result-set)
                      :result-set/score (params->score params)}])
    {:status  303
     :headers {"Location" (str "/app/results/" (:id path-params))}}))


(defn edit
  [{:keys [biff/db session path-params]}]
  (let [{:result/keys [type] :as result}
        (first (biff/q db '{:find  (pull result [* {:result/type [* {:result/workout [*]}]}])
                            :in    [[result-id user]]
                            :where [[result :xt/id result-id]
                                    [result :result/user user]]}
                       [(parse-uuid (:id path-params)) (:uid session)]))
        result-path (str "/app/results/" (:xt/id result))]
    [:div#edit-result
     (result-form
       (assoc {}
              :result type
              :workout (:result/workout type)
              :action result-path
              :hx-key :hx-put
              :form-props {:hx-target "closest #result-ui"
                           :hx-swap   "outerHTML"})
       [:button.btn {:type "submit"} "Update Result"]
       [:button.btn.bg-red-400.hover:bg-red-600 {:hx-get    result-path
                                                 :hx-target "closest #edit-result"} "Cancel"])]))


(defn index
  [{:keys [biff/db session] :as _ctx}]
  (let [results (biff/q db '{:find  (pull result [* {:result/type [* {:result/workout [*]}]}])
                             :in    [[user]]
                             :where [[result :result/user user]]}
                        [(:uid session)])]
    (ui/page {} [:div
                 [:h1 "Results"]
                 [:ul
                  (map (fn [r]
                         [:li
                          (result-ui r)]) results)]])))


(defn create
  [{:keys [biff/db session params] :as ctx}]
  (let [workout-id (parse-uuid (:workout params))
        {:workout/keys [reps-per-round] :as _workout} (xt/entity db workout-id)]
    (biff/submit-tx ctx
                    [{:db/op          :create
                      :db/doc-type    :wod-result
                      :xt/id          :db.id/wod-result
                      :result/workout workout-id
                      :result/scale   (keyword (:scale params))
                      :result/notes   (:notes params)}
                     {:db/op       :merge
                      :db/doc-type :result
                      :result/user (:uid session)
                      :result/date (instant/read-instant-date (:date params))
                      :result/type :db.id/wod-result}
                     {:db/op             :create
                      :db/doc-type       :wod-set
                      :result-set/score  (params->score (assoc params :reps-per-round reps-per-round))
                      :result-set/parent :db.id/wod-result}]))
  (let [{:xt/keys [id] :as w} (xt/entity db (parse-uuid (:workout params)))]
    {:status  303
     :headers {"location" (str "/app/workouts/" id)}}))


(defn new
  [{:keys [biff/db session params] :as ctx}]
  (let [workout (first (biff/q db
                               '{:find (pull workout [*])
                                 :in [[name]]
                                 :where [[workout :workout/name name]]}
                               [(:workout params)]))]

    (ui/page {} [:div.pb-8
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
   ["/:id"
    ["" {:get (wildcard-override show {:new new})
         :put update-handler}]
    ["/edit" {:get edit}]]])
