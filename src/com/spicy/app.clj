(ns com.spicy.app
  (:require
    [com.spicy.middleware :as mid]
    [com.spicy.movements.core :as movements]
    [com.spicy.results.core :as results]
    [com.spicy.settings :as settings]
    [com.spicy.ui :as ui]
    [com.spicy.workouts.core :as workouts]
    [xtdb.api :as xt]))


(defn app
  [{:keys [session biff/db] :as ctx}]
  (let [{:user/keys [email foo bar]} (xt/entity db (:uid session))]
    (ui/page {} [:div "TODO"])))


(def about-page
  (ui/page
    {:base/title (str "About " settings/app-name)}
    [:p "This app was made with "
     [:a.link {:href "https://biffweb.com"} "Biff"] "."]))


(def plugin
  {:static {"/about/" about-page}
   :routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["" {:get app}]
            workouts/routes
            results/routes
            movements/routes]
   :api-routes []})
