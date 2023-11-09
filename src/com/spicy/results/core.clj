(ns com.spicy.results.core
  (:require
    [clojure.instant :as instant]
    [com.biffweb :as biff]
    [com.spicy.calendar :as c]
    [com.spicy.middleware :as mid]
    [com.spicy.movements.ui :refer [strength-set-inputs]]
    [com.spicy.results.score :refer [scores->tx ->scores]]
    [com.spicy.results.transform :as t]
    [com.spicy.results.ui :refer [result-ui result-form inline-result-ui]]
    [com.spicy.route-helpers :refer [wildcard-override]]
    [com.spicy.ui :as ui]
    [java-time.api :as jt]
    [xtdb.api :as xt]))


(defn workouts-referer?
  [{:keys [headers] :as _ctx}]
  (let [referer (get headers "referer")]
    (re-matches #".*/workouts/.*" referer)))


(defn show
  [{:keys [biff/db session path-params] :as ctx}]
  (let [result (first (biff/q db '{:find (pull result [* {:result/type [*
                                                                        {:result/workout [*]}
                                                                        {:result/movement [*]}
                                                                        {:result-set/_parent [*]}]}])
                                   :in [[result-id user]]
                                   :where [[result :xt/id result-id]
                                           [result :result/user user]]}
                              [(parse-uuid (:id path-params)) (:uid session)]))]
    (if (workouts-referer? ctx)
      (inline-result-ui result)
      (result-ui result))))


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
                               {:scheme          (:workout/scheme workout)
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
     [:h2.text-2xl.font-bold (:name (t/normalized-result result))]
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
                      :value (if (:result/date result)
                               (biff/format-date
                                 (:result/date result) "YYYY-MM-dd")
                               (jt/format  "YYYY-MM-dd" (jt/zoned-date-time (jt/zone-id "America/Boise"))))}]
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


(def list-icon [:svg.w-6.h-6.flex-none {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24" :stroke-width "1" :stroke "currentColor"} [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"}]])
(def calendar-icon [:svg.w-6.h-6.flex-none {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"} [:path {:stroke-linecap "round" :stroke-linejoin "round" :d "M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5m-9-6h.008v.008H12v-.008zM12 15h.008v.008H12V15zm0 2.25h.008v.008H12v-.008zM9.75 15h.008v.008H9.75V15zm0 2.25h.008v.008H9.75v-.008zM7.5 15h.008v.008H7.5V15zm0 2.25h.008v.008H7.5v-.008zm6.75-4.5h.008v.008h-.008v-.008zm0 2.25h.008v.008h-.008V15zm0 2.25h.008v.008h-.008v-.008zm2.25-4.5h.008v.008H16.5v-.008zm0 2.25h.008v.008H16.5V15z"}]])


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
        results          (map second date-and-results)]
    (ui/page ctx (ui/panel [:div {:class (str "p-4")}
                            [:div.flex.justify-between
                             [:h1.text-3xl.cursor-default.capitalize.text-center.sm:text-left "Results"]
                             [:div
                              {:role             "tablist"
                               :class (str "flex space-x-1 bg-slate-100 p-0 5 border border-black min-w-[95px]")
                               :aria-orientation "horizontal"
                               :id               "results-view-toggle"}
                              [:button#headlessui-tabs-tab-8
                               {:class                 '[flex items-center w-full pl-2 pr-2 text-base lg:pr-3 bg-brand-teal]
                                :role                  "tab"
                                :type                  "button"
                                :aria-selected         "true"
                                :tabindex              "0"
                                :data-headlessui-state "selected"
                                :aria-controls         "headlessui-tabs-panel-10"}
                               list-icon
                               [:span.sr-only.lg:not-sr-only.lg:ml-2.text-black.text-base "List"]]
                              [:button#headlessui-tabs-tab-9
                               {:class                 '[flex items-center w-full pl-2 pr-2 text-base lg:pr-3]
                                :role                  "tab"
                                :type                  "button"
                                :aria-selected         "false"
                                :tabindex              "-1"
                                :data-headlessui-state ""
                                :hx-get                "/app/results/calendar"
                                :hx-target             "#results-view-toggle"
                                :hx-select             "#results-view-toggle"
                                :hx-swap               "outerHTML"
                                :hx-select-oob         "#results-panel"
                                :aria-controls "headlessui-tabs-panel-11"}
                               calendar-icon
                               [:span.sr-only.lg:not-sr-only.lg:ml-2.text-slate-900 "Calendar"]]]]
                            [:div {:id "results-panel"}
                             [:ul.list-none.p-0.m-0.mt-4.space-y-4.max-w-xl.sm:mx-auto
                              (map (fn [r]
                                     [:li.p-4.border-2.border-black.bg-white
                                      (result-ui r)]) results)]]]))))


(defn calendar-view
  [{:keys [params session biff/db] :as _ctx}]
  (let [date       (if (:date params)
                     (apply jt/local-date (c/safe-parse-date (:date params)))
                     (jt/zoned-date-time (jt/zone-id "America/Boise")))
        start-date (c/time-frame-start-day date)
        end-date   (jt/plus start-date (jt/days 42))
        results    (map second (biff/q db '{:find     [d (pull r [*
                                                                  {:result/type
                                                                   [*
                                                                    {:result/workout [*]}
                                                                    {:result/movement [*]}
                                                                    {:result-set/_parent [*]}]}])]
                                            :in       [[user start end]]
                                            :where    [[r :result/user user]
                                                       [r :result/date d]
                                                       [(>= d start)]
                                                       [(<= d end)]]
                                            :order-by [[d :desc]]}
                                       [(:uid session) (c/->date start-date) (c/->date end-date)]))]
    [:<>
     [:div
      {:role             "tablist"
       :class (str " flex sm:space-x-1 bg-slate-100 p-0 5 border border-black min-w-[95px]")
       :aria-orientation "horizontal"
       :id               "results-view-toggle"}
      [:button#headlessui-tabs-tab-8
       {:class                 '[flex items-center w-full pl-2 pr-2 text-base lg:pr-3]
        :role                  "tab"
        :type                  "button"
        :aria-selected         "true"
        :tabindex              "0"
        :data-headlessui-state "selected"
        :hx-get                "/app/results"
        :hx-target             "#results-view-toggle"
        :hx-select             "#results-view-toggle"
        :hx-swap               "outerHTML"
        :hx-select-oob         "#results-panel"
        :aria-controls         "headlessui-tabs-panel-10"}
       list-icon
       [:span.sr-only.lg:not-sr-only.lg:ml-2.text-black.text-base "List"]]
      [:button#headlessui-tabs-tab-9
       {:class                 '[flex items-center w-full m-0 pl-2 pr-2 text-base lg:pr-3 bg-brand-teal]
        :role                  "tab"
        :type                  "button"
        :aria-selected         "false"
        :tabindex              "-1"
        :data-headlessui-state ""
        :aria-controls         "headlessui-tabs-panel-11"}
       calendar-icon
       [:span.sr-only.lg:not-sr-only.lg:ml-2.text-black.text-base "Calendar"]]]
     [:div {:id "results-panel"}
      (c/calendar {:date    date
                   :today   (jt/zoned-date-time (jt/zone-id "America/Boise"))
                   :results results})]]))


(defn create
  [{:keys [biff/db session params] :as ctx}]
  (let [workout-id                       (parse-uuid (:workout params))
        {:workout/keys
         [scheme
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
                                                    {:scheme          scheme
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
   ["/:id" {:middleware [(partial mid/wrap-ensure-owner #{"new" "calendar" "calendar-day"})]}
    ["" {:get (wildcard-override show {:new new
                                       :calendar calendar-view
                                       :calendar-day c/day-view})
         :put update-handler}]
    ["/edit" {:get edit}]]])
