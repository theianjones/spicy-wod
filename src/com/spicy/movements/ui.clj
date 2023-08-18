(ns com.spicy.movements.ui
  (:require
    [com.biffweb :as biff]))


(defn movement-form
  [{:keys [movement]}]
  (biff/form
    (merge {:class     "flex flex-col gap-4"
            :hx-target :body}
           (if (nil? movement)
             {:hx-post "/app/movements"}
             {:hx-put (str "/app/movements/" (:xt/id movement))}))
    [:div.flex.flex-col.w-full
     [:label {:for :name} "Title"]
     [:input#name
      {:placeholder "Name"
       :name        "name"
       :class       (str "pink-input p-2 outline-none focus-teal")
       :required    true
       :value       (:movement/name movement)}]]
    [:div.flex.flex-col.w-full
     [:label {:for :type} "Type"]
     [:select.pink-input.teal-focus#type
      {:name      "type"
       :required  true
       :value     (when (not (nil? movement)) (name (:movement/type movement)))}
      [:option {:value "" :label "--Select a Movement type--"}]
      [:option {:value "strength"
                :label "Strength"}]
      [:option {:value "gymnastic"
                :label "Gymnastic"}]
      [:option {:value "monostructural"
                :label "Monostructural"}]]]
    [:button.btn.bg-brand-teal {:type "submit"} (if (nil? movement) "Create movement" "Update movement")]))


(defn strength-set-inputs
  [{:keys [number reps weight status id]}]
  [:div
   [:p.text-2xl.font-medium.text-center.mt-4.whitespace-nowrap (str "Set #" number)]
   [:input {:value reps
            :type  "hidden"
            :id    (str "reps-" number)
            :name  (str "reps-" number)}]
   [:.flex.justify-center.items-center.m-0
    [:fieldset
     [:.flex.gap-2
      [:label.text-lg.sm:text-2xl.font-medium.text-center.h-fit.my-auto
       {:for (str "hit-" number)}
       "Hit"]
      (when id
        [:input {:name  (str "id-" number)
                 :id    (str "id-" number)
                 :type  :hidden
                 :value id}])
      [:input {:name    (str "hit-miss-" number)
               :id      (str "hit-" number)
               :class   (str "appearance-none p-7 border-2 border-r-0 border-black cursor-pointer "
                             "checked:bg-brand-teal checked:text-brand-teal checked:color-brand-teal hover:bg-brand-teal checked:border-black checked:ring-0 checked:ring-offset-0 checked:ring-brand-teal checked:ring-opacity-100 focus:outline-none focus:ring-0 focus:ring-offset-0 focus:ring-opacity-100")
               :value   :hit
               :checked (= :pass status)
               :type    :checkbox}]]]
    [:input {:name     (str "weight-" number)
             :id       (str "weight-" number)
             :class    (str "p-4 border-2 border-black w-1/2 text-center font-bold teal-focus ")
             :required true
             :value    weight
             :type     :number}]
    [:p.m-0.bg-white.p-4.border-2.border-l-0.border-black.font-medium.whitespace-nowrap (str "x " reps " reps")]]])


(defn movement-results-form
  [opts {:result/keys [date notes]} & children]
  (biff/form opts
             children
             [:div.flex.flex-col.justify-center.items-center.gap-4
              [:input.pink-input.teal-focus.mt-4.mx-auto
               {:type  "date"
                :name  "date"
                :value (biff/format-date
                         (or date (biff/now)) "YYYY-MM-dd")}]
              [:textarea#notes
               {:name        "notes"
                :placeholder "notes"
                :rows        7
                :value       notes
                :class       (str "w-full pink-input teal-focus")}]
              [:button.btn "Submit"]]))
