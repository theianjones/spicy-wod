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