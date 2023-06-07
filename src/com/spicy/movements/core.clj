(ns com.spicy.movements.core
  (:require
    [clojure.string :as string]
    [com.biffweb :as biff]
    [com.biffweb.impl.rum :as brum]
    [com.spicy.ui :as ui]
    [rum.server-render :as sr]))


(defn htmx-request?
  [ctx]
  (-> ctx :headers
      (get "hx-request")
      (= "true")))


(defn movements-list
  [movements]
  [:ul#movement-list.list-none
   (map (fn [m]
          [:li
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
                             [:div
                              [:h1 "Movements"]
                              [:select {:name      "type"
                                        :onchange "window.open('?type=' + this.value,'_self')"}
                               [:option {:value :strength :selected (or (= (:type params) "stregnth") (empty? (:type params)))} "Strength"]
                               [:option {:value :gymnastic :selected (= (:type params) "gymnastic")} "Gymnastic"]
                               [:option {:value :monostructural :selected (= (:type params) "monostructural")} "Cardio"]]]
                             (movements-list movements)])))))


(defn show
  [{:keys [biff/db path-params]}]
  (let [m (first (biff/q db '{:find (pull m [*
                                             {:workout-movement/_movement
                                              [{:workout-movement/workout [*]}]}])
                              :in [id]
                              :where [[m :xt/id id]]}
                         (parse-uuid (:id path-params))))
        workouts (->> m
                      :workout-movement/_movement
                      (map :workout-movement/workout))]
    (ui/page {} (ui/panel
                  [:div
                   [:h1.text-3xl.cursor-default.capitalize.mb-14 (:movement/name m)]
                   [:h2.text-xl.mb-4 "Related workouts"]
                   [:div.flex.gap-2.sm:gap-4.flex-wrap.justify-center.pb-20
                    (map movement-workout-ui workouts)]]))))


(def routes
  ["/movements"
   ["/" {:get index}]
   ["/:id" {:get show}]])
