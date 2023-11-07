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


(def StrengthSet
  [:map
   [:xt/id :strength-set/id]
   [:result-set/parent :strength-result/id]
   [:result-set/number :int]
   [:result-set/reps :int]
   [:result-set/status [:enum :pass :fail]]
   [:result-set/weight :int]])


(def MonostructuralSet
  [:map
   [:xt/id :mono-set/id]
   [:result-set/parent :mono-result/id]
   [:result-set/number :int]
   [:result-set/distance :int]
   [:result-set/time :int]])


(def WodSet
  [:map
   [:xt/id :wod-set/id]
   [:result-set/score float?]
   [:result-set/number :int]
   [:result-set/parent :wod-result/id]])


(def WodResult
  [:map
   [:xt/id :wod-result/id]
   [:result/workout :workout/id]
   [:result/notes {:optional true} :string]
   [:result/scale [:enum :rx :scaled :rx+]]])


(def StrengthResult
  [:map
   [:xt/id :strength-result/id]
   [:result/movement :movement/id]
   [:result/notes {:optional true} :string]
   [:result/set-count :int]])


(def MonostructuralResult
  [:map
   [:xt/id :mono-result/id]
   [:result/movement :movement/id]
   [:result/notes {:optional true} :string]
   [:result/distance :int]
   [:result/time :int]])


(def Result
  [:map {:closed true}
   [:xt/id :result/id]
   [:result/user :user/id]
   [:result/date inst?]
   [:result/type :uuid]])


(def schema
  {:user/id :uuid
   :user [:map {:closed true}
          [:xt/id :user/id]
          [:user/email :string]
          [:user/joined-at inst?]]

   :msg/id :uuid
   :msg [:map {:closed true}
         [:xt/id :msg/id]
         [:msg/user :user/id]
         [:msg/text :string]
         [:msg/sent-at inst?]]
   :workout/id :uuid
   :workout [:map {:closed true}
             [:xt/id :workout/id]
             [:workout/name :string]
             [:workout/description :string]
             [:workout/scheme workout-types]
             [:workout/created-at {:optional true} inst?]
             [:workout/reps-per-round {:optional true} :int]
             [:workout/rounds-to-score {:optional true} :int]
             [:workout/user {:optional true} :user/id]
             [:workout/sugar-id {:optional true} :string]
             [:workout/tiebreak-scheme {:optional true} [:enum :time :reps]]
             [:workout/secondary-scheme {:optional true} (into [] (filter #(not (= :time-with-cap %)) workout-types))]]
   :strength-set/id :uuid
   :strength-set StrengthSet
   :mono-set/id :uuid
   :mono-set MonostructuralSet
   :wod-result/id :uuid
   :wod-result WodResult
   :wod-set/id :uuid
   :wod-set WodSet
   :strength-result/id :uuid
   :strength-result StrengthResult
   :mono-result/id :uuid
   :mono-result MonostructuralResult
   :result/id :uuid
   :result Result
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
