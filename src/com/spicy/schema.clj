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
   [:result-set/reps :int]
   [:result-set/status [:enum :pass :fail]]
   [:result-set/weight :int]])


(def MonostructuralSet
  [:map
   [:xt/id :mono-set/id]
   [:result-set/distance :int]
   [:result-set/time :int]])


(def ResultSet
  [:map {:closed true}
   [:xt/id :result-set/id]
   [:result-set/result :result/id]
   [:result-set/type :uuid]])


(def WodResult
  [:map
   [:xt/id :wod-result/id]
   [:result/workout :workout/id]
   [:result/score :string]
   [:result/date inst?]
   [:result/scale [:enum :rx :scaled :rx+]]
   [:result/notes {:optional true} :string]
   [:result/tie-break {:optional true} [:or inst? :string]]])


(def StrengthResult
  [:map
   [:xt/id :strength-result/id]
   [:result/movement :movement/id]
   [:result/set-count :int]
   [:result/notes {:optional true} :string]])


(def MonostructuralResult
  [:map
   [:xt/id :mono-result/id]
   [:result/movement :movement/id]
   [:result/distance :int]
   [:result/time :int]
   [:result/notes {:optional true} :string]])


(def Result
  [:map {:closed true}
   [:xt/id :result/id]
   [:result/user :user/id]
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
             [:workout/sets {:optional true} :int]
             [:workout/number {:optional true} :int]
             [:workout/user {:optional true} :user/id]
             [:workout/tiebreak-scheme {:optional true} [:enum :time :reps]]
             [:workout/secondary-scheme {:optional true} (into [] (filter #(not (= :time-with-cap %)) workout-types))]]
   :result-set/id :uuid
   :result-set ResultSet
   :strength-set/id :uuid
   :strength-set StrengthSet
   :mono-set/id :uuid
   :mono-set MonostructuralSet
   :wod-result/id :uuid
   :wod-result WodResult
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
