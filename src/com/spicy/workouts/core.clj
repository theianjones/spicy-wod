(ns com.spicy.workouts.core
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.spicy.route-helpers :refer [new-or-show]]
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
  (let [workout (first (biff/q db '{:find  (pull workout [*])
                                    :in    [[name id]]
                                    :where [(or [workout :workout/name name]
                                                [workout :xt/id id])]}
                               [(string/capitalize (:id path-params))
                                (parse-uuid (:id path-params))]))]
    (if (true? (:fragment params))
      (ui/workout-ui workout)
      (ui/page {} [:div.pb-8
                   (ui/panel [:div {:class (str "h-full flex flex-col gap-6 md:flex-row pb-20")}
                              [:div.flex.gap-2.flex-col
                               (workout-logbook-ui workout)
                               [:a {:href  (str "/app/results/new?workout=" (:workout/name workout))
                                    :class (str "btn h-fit w-fit place-self-center")} "Log workout"]]
                              [:.flex-1
                               (workout-results
                                 {:user    (:uid session)
                                  :workout (:xt/id workout)
                                  :biff/db db})]])]))))


(defn new
  [{:keys [biff/db path-params session params] :as _ctx}]
  (let [fragment? (:fragment params)]
    (if fragment?
      (workout-form {:hidden {:fragment (str (true? fragment?))}})

      (ui/page {}
               [:div.max-w-md.mx-auto
                (ui/panel
                  [:div.p-4
                   [:h1.text-5xl.mb-14.pt-8.text-center "New Workout"]
                   (workout-form {:hidden {:fragment (str (true? fragment?))}})])]))))


(defn create
  [{:keys [biff/db path-params session params] :as ctx}]
  (let [workout-uuid (random-uuid)]
    (biff/submit-tx ctx [{:db/op               :merge
                          :db/doc-type         :workout
                          :xt/id               workout-uuid
                          :workout/name        (:name params)
                          :workout/user        (:uid session)
                          :workout/scheme      (keyword (:scheme params))
                          :workout/description (:description params)}])
    {:status  303
     :headers {"location" (str "/app/workouts/" workout-uuid "?fragment=" (true?  (:fragment params)))}}))


(def routes
  ["/workouts"
   ["" {:get index
        :post create}]
   ["/:id" {:get (new-or-show new show)}]])
