(ns com.spicy.app
  (:require
    [clojure.java.io :as io]
    [com.biffweb :as biff :refer [q]]
    [com.spicy.middleware :as mid]
    [com.spicy.movements.core :as movements]
    [com.spicy.results.core :as results]
    [com.spicy.results.ui :as r]
    [com.spicy.settings :as settings]
    [com.spicy.sugarwod.core :as sugar.core]
    [com.spicy.sugarwod.csv :as csv]
    [com.spicy.ui :as ui]
    [com.spicy.workouts.core :as workouts]
    [com.spicy.workouts.ui :as w]))


(defn app
  [{:keys [session biff/db] :as ctx}]
  (let [wod-results (map second (q db '{:find [d (pull r [*
                                                          {:result/type
                                                           [*
                                                            {:result/workout [*]}
                                                            {:result-set/_parent [*]}]}])]
                                        :in [[user]]
                                        :where [[r :result/user user]
                                                [r :result/type type]
                                                [r :result/date d]
                                                [type :result/workout]]
                                        :limit 5
                                        :order-by [[d :desc]]}
                                   [(:uid session)]))
        strength-results (map second (q db '{:find [d (pull r [*
                                                               {:result/type
                                                                [*
                                                                 {:result/movement [*]}
                                                                 {:result-set/_parent [*]}]}])]
                                             :in [[user]]
                                             :where [[r :result/user user]
                                                     [r :result/type type]
                                                     [r :result/date d]
                                                     [type :result/movement]]
                                             :limit 5
                                             :order-by [[d :desc]]}
                                        [(:uid session)]))]
    (ui/page
      ctx
      (ui/panel [:div.flex.gap-4.flex-col.sm:flex-row
                 [:div.p-4.border-2.border-black.bg-white
                  [:h2.text-2xl.font-bold "Latest WODs"]
                  (if (zero? (count wod-results))
                    [:div.flex.flex-col.items-center
                     [:p "Log a WOD result to see it show up here!"]
                     [:a.btn {:href "/app/workouts"} "View Workouts"]]
                    [:div (map (fn [result]
                                 (let [{:keys [workout name date] :as normalized} (r/normalized-result result)]
                                   [:div.mb-2
                                    [:a.text-lg.font-sans {:href (str "/app/workouts/" (:xt/id workout))} [:h3.font-sans name]]
                                    [:div
                                     [:span.font-bold (w/display-summed-score normalized)]
                                     [:span.text-gray-700.ml-2 date]]])) wod-results)])]
                 [:div.p-4.border-2.border-black.bg-white
                  [:h2.text-2xl.font-bold "Latest Lifts"]
                  (if (zero? (count strength-results))
                    [:div.flex.flex-col.items-center
                     [:p  "Log a lift to see it show up here!"]
                     [:a.btn {:href "/app/movements"} "View Movements"]]

                    [:div (map (fn [result]
                                 (let [{:keys [movement name date sets] :as normalized} (r/normalized-result result)]
                                   [:div.mb-2
                                    [:a.text-lg.font-sans {:href (str "/app/movements/" (:xt/id movement))} [:h3.font-sans.capitalize name]]
                                    [:div
                                     [:span.font-bold (apply max (map #(if (= :pass (:result-set/status %))
                                                                         (:result-set/weight %)
                                                                         0) sets))]
                                     [:span.text-gray-700.ml-2 date]]])) strength-results)])]]))))


(def about-page
  (ui/page
    {:base/title (str "About " settings/app-name)}
    [:p "This app was made with "
     [:a.link {:href "https://biffweb.com"} "Biff"] "."]))


(defn import-page
  [ctx]
  (ui/page
    ctx
    (ui/panel [:h1.text-2xl.font-bold.mb-8
               "Import your data"]
              [:h2.text-xl.font-bold.mb-4
               "SugarWOD"]
              (biff/form {:method :post :enctype "multipart/form-data"}
                         [:input {:type :file :name :file :id :file}]
                         [:button.btn {:type "submit"} "Submit"]))))


(defn process-import
  [{:keys [params session] :as ctx}]
  (let [user (:uid session)
        data (csv/parse-sugar-csv (io/reader (-> params :file :tempfile)))
        tx-data (sugar.core/sugar-movements->tx-data (assoc ctx :user user) data)]
    (biff/submit-tx ctx tx-data))
  {:status  303
   :headers {"location" "/app/import"}})


(def plugin
  {:static {"/about/" about-page}
   :routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["" {:get app}]
            ["/import" {:get import-page
                        :post process-import}]
            workouts/routes
            results/routes
            movements/routes]
   :api-routes []})
