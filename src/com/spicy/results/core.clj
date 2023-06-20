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
  (let [result (first (biff/q db '{:find (pull result [* {:result/workout [*]}])
                                   :in [[result-id user]]
                                   :where [[result :xt/id result-id]
                                           [result :result/user user]]}
                              [(parse-uuid (:id path-params)) (:uid session)]))]
    (result-ui result)))


(defn update-handler
  [{:keys [biff/db params session path-params] :as ctx}]

  (let [result (first (biff/q db '{:find (pull result [* {:result/workout [*]}])
                                   :in [[result-id user]]
                                   :where [[result :xt/id result-id]
                                           [result :result/user user]]}
                              [(parse-uuid (:id path-params)) (:uid session)]))
        tx (biff/submit-tx ctx
                           [{:db/op :update
                             :db/doc-type :result
                             :xt/id (parse-uuid (:id path-params))
                             :result/user (:uid session)
                             :result/score (params->score params)
                             :result/notes (:notes params)
                             :result/scale (keyword (:scale params))
                             :result/date (instant/read-instant-date (:date params))}])]
    {:status 303
     :headers {"Location" (str "/app/results/" (:id path-params))}}))


(defn edit
  [{:keys [biff/db session path-params]}]
  (let [{:result/keys [workout] :as result}
        (first (biff/q db '{:find  (pull result [* {:result/workout [*]}])
                            :in    [[result-id user]]
                            :where [[result :xt/id result-id]
                                    [result :result/user user]]}
                       [(parse-uuid (:id path-params)) (:uid session)]))
        result-path (str "/app/results/" (:xt/id result))]
    [:div#edit-result
     (result-form
       (assoc {}
              :result result
              :workout workout
              :action result-path
              :hx-key :hx-put
              :form-props {:hx-target "closest #result-ui"
                           :hx-swap   "outerHTML"})
       [:button.btn {:type "submit"} "Update Result"]
       [:button.btn.bg-red-400.hover:bg-red-600 {:hx-get    result-path
                                                 :hx-target "closest #edit-result"} "Cancel"])]))


(defn index
  [{:keys [biff/db session] :as _ctx}]
  (let [results (biff/q db '{:find (pull result [* {:result/workout [:workout/name]}])
                             :in [[user]]
                             :where [[result :result/user user]]}
                        [(:uid session)])]
    (ui/page {} [:div
                 [:h1 "Results"]
                 [:ul
                  (map (fn [r]
                         (let [workout-name (get-in r [:result/workout :workout/name])]
                           [:li
                            (result-ui r)])) results)]])))


(defn create
  [{:keys [biff/db session params] :as ctx}]
  (biff/submit-tx ctx
                  [{:db/op          :create
                    :db/doc-type    :wod-result
                    :xt/id          :db.id/wod-result
                    :result/workout (parse-uuid (:workout params))
                    :result/score   (params->score params)
                    :result/notes   (:notes params)
                    :result/scale   (keyword (:scale params))
                    :result/date    (instant/read-instant-date (:date params))}
                   {:db/op       :merge
                    :db/doc-type :result
                    :result/user (:uid session)
                    :result/type :db.id/wod-result}])
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
