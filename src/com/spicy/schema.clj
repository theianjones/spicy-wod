(ns com.spicy.schema)


(def workout-types
  [:enum
   :time
   :time-with-cap
   :pass-fail
   :rounds-reps
   :reps
   :emom
   :load
   :calories
   :meters
   :feet
   :points])


(def schema
  {:user/id :uuid
   :user [:map {:closed true}
          [:xt/id                     :user/id]
          [:user/email                :string]
          [:user/joined-at            inst?]]

   :msg/id :uuid
   :msg [:map {:closed true}
         [:xt/id       :msg/id]
         [:msg/user    :user/id]
         [:msg/text    :string]
         [:msg/sent-at inst?]]
   :workout/id :uuid
   :workout [:map {:closed true}
             [:xt/id :workout/id]
             [:workout/name :string]
             [:workout/description :string]
             [:workout/scheme workout-types]
             [:workout/user {:optional true} :user/id]
             [:workout/tiebreak-scheme {:optional true} [:enum :time :reps]]
             [:workout/secondary-scheme {:optional true} (into [] (filter #(not (= :time-with-cap %)) workout-types))]]
   :result/id :uuid
   :result [:map {:closed true}
            [:xt/id :result/id]
            [:result/user :user/id]
            [:result/workout :workout/id]
            [:result/score :string]
            [:result/date inst?]
            [:result/notes {:optional true} :string]
            [:result/tie-break {:optional true} [:or inst? :string]]
            [:result/scale [:enum :rx :scaled :rx+]]]
   :movement/id :uuid
   :movement [:map {:closed true}
              [:xt/id :movement/id]
              [:movement/name :string]
              [:movement/type [:enum :strength :gymnastic :monostructural]]]
   :workout-movement/id :uuid
   :workout-movement [:map {:closed true}
                      [:xt/id :workout-movement/id]
                      [:workout-movement/workout :workout/id]
                      [:workout-movement/movement :movement/id]]})


(def plugin
  {:schema schema})
