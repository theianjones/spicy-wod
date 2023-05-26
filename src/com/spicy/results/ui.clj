(ns com.spicy.results.ui
  (:require
    [cheshire.core :as cheshire]
    [clojure.instant :as instant]
    [clojure.string :as string]
    [com.biffweb :as biff :refer [q]]
    [com.spicy.middleware :as mid]
    [com.spicy.route-helpers :refer [new-or-show]]
    [com.spicy.settings :as settings]
    [com.spicy.ui :as ui]
    [com.spicy.workouts.core :as workouts]
    [ring.adapter.jetty9 :as jetty]
    [rum.core :as rum]
    [xtdb.api :as xt]))


(defn result-ui
  [{:result/keys [score workout] :as result}]
  [:div#result-ui
   [:div
    [:a {:href (str "/app/workouts/" (string/lower-case (:workout/name workout)))} (:workout/name workout)]]
   [:div score]
   [:button.btn {:hx-get (str "/app/results/" (:xt/id result) "/edit")
                 :hx-target "closest #result-ui"} "Edit"]])


(defn scheme-forms
  [{:workout/keys [scheme _secondary-scheme] :result/keys [score]}]
  (case scheme
    :time (let [[minutes seconds] (string/split (or score ":") #":")]
            [:div.flex.gap-3
             [:input.w-full.pink-input.teal-focus#minutes
              {:type        "text"
               :name        "minutes"
               :placeholder "Minutes"
               :value       minutes}]
             [:input.w-full.pink-input.teal-focus#seconds
              {:type        "text"
               :name        "seconds"
               :placeholder "Seconds"
               :value       seconds}]])
    :time-with-cap "todo"
    :pass-fail [:input.w-full#reps
                {:type "text" :name "reps" :placeholder "Rounds Successfully Completed" :value score}]
    :rounds-reps
    (let [[rounds reps] (string/split (or score "+") #"\+")]
      [:div.flex.gap-3
       [:input.w-full.pink-input.teal-focus#rounds
        {:type "text" :name "rounds" :placeholder "Rounds" :value rounds}]
       [:input.w-full.pink-input.teal-focus#reps
        {:type "text" :name "reps" :placeholder "Reps" :value reps}]])
    [:input.w-full#reps {:type "text" :name "reps" :placeholder "Reps" :value score}]))


(defn result-form
  [{:keys [workout result action hidden hx-key form-props] :as props} & children]
  (biff/form
    (merge (or form-props {})
           {(or hx-key :action) action
            :class              "flex flex-col gap-3"
            :hidden             hidden})
    (scheme-forms (merge workout result))
    [:input.pink-input.teal-focus
     {:type  "date"
      :name  "date"
      :value (biff/format-date
               (or (:result/date result) (biff/now)) "YYYY-MM-dd")}]
    [:div.flex.gap-2.items-center
     [:div.flex-1.flex.gap-2.items-center
      [:input#rx {:type    "radio"
                  :name    "scale"
                  :value   "rx"
                  :checked (= (:result/scale result) :rx)}]
      [:label {:for "rx"} "Rx"]]
     [:div.flex-1.flex.gap-2.items-center
      [:input#scaled {:type    "radio"
                      :name    "scale"
                      :value   "scaled"
                      :checked (= (:result/scale result) :scaled)}]
      [:label {:for "scaled"} "Scaled"]]
     [:div.flex-1.flex.gap-2.items-center
      [:input#rx+ {:type    "radio"
                   :name    "scale"
                   :value   "rx+"
                   :checked (= (:result/scale result) :rx+)}]
      [:label {:for "rx+"} "Rx+"]]]
    [:textarea.w-full.pink-input.teal-focus#notes {:name        "notes"
                                                   :placeholder "notes"
                                                   :value       (:result/notes result)}]
    children))


(defn params->score
  [{:keys [minutes seconds rounds reps]}]
  (cond
    (or (some? minutes) (some? seconds)) (str minutes ":" seconds)
    (and (some? rounds) (some? reps)) (str rounds "+" reps)
    (some? reps) reps
    :else nil))
