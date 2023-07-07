(ns com.spicy.repl
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [com.biffweb :as biff :refer [q]]
    [com.spicy :as main]
    [xtdb.api :as xt]))


(defn get-context
  []
  (biff/assoc-db @main/system))


(defn add-fixtures
  [edn-file]
  (biff/submit-tx (get-context)
                  (-> (io/resource edn-file)
                      slurp
                      edn/read-string)))


(defn get-results
  [{:keys [biff/db]} user]
  (biff/q db '{:find  (pull result [* {:result/type [* {:result/workout [*]}]}])
               :in    [[user]]
               :where [[result :result/user user]]}
          [user]))


(defn scary-reset-db!
  []
  (let [{:keys [biff/db] :as ctx} (get-context)
        ids (biff/q db '{:find [e]
                         :where [[e :xt/id]]})]
    (biff/submit-tx ctx (map (fn [[id]]
                               {:db/op :delete
                                :xt/id id}) ids))
    (add-fixtures "workouts.edn")
    (add-fixtures "movements.edn")))


(def user-a
  (first (let [{:keys [biff/db]} (get-context)]
           (biff/q db '{:find u
                        :where [[u :user/email "user.a@example.com"]]}))))


(comment
  ;; Call this in dev if you'd like to add some seed data to your database. If
  ;; you edit the seed data (in resources/fixtures.edn), you can reset the
  ;; database by running `rm -r storage/xtdb` (DON'T run that in prod),
  ;; restarting your app, and calling add-fixtures again.
  (add-fixtures "workouts.edn")
  (add-fixtures "movements.edn")

  (def user-a  #uuid "e6af46b4-b20e-4782-9de9-8d56f8d0a3a0")
  (def mu-movement #uuid "078726e4-1225-40e9-a48a-edb38d38dfa8")

  (biff/submit-tx (get-context)
                  [{:xt/id       :db.id/result
                    :db/doc-type :result
                    :db/op       :create
                    :result/user user-a
                    :result/type :db.id/stength-result}
                   {:xt/id             :db.id/strength-result
                    :db/doc-type       :strength-result
                    :result/movement   mu-movement
                    :result/set-count  3
                    }
                   {:db/doc-type       :strength-set
                    :result-set/parent :db.id/strength-result
                    :result-set/status :pass
                    :result-set/reps   5
                    :result-set/weight 20}
                   {:db/doc-type       :strength-set
                    :result-set/parent :db.id/strength-result
                    :result-set/result :db.id/result
                    :result-set/status :pass
                    :result-set/reps   5
                    :result-set/weight 15}
                   {:db/doc-type       :strength-set
                    :result-set/parent :db.id/strength-result

                    :result-set/status :fail
                    :result-set/reps   5
                    :result-set/weight 15}])
  (let [{:keys [biff/db] :as ctx} (get-context)
        ids (biff/q db '{:find [e]
                         :where [[e :xt/id]]})]
    (biff/submit-tx ctx (map (fn [[id]]
                               {:db/op :delete
                                :xt/id id}) ids)))

  (biff/submit-tx (get-context)
                  [{:xt/id       :db.id/result
                    :db/doc-type :result
                    :db/op       :create
                    :result/user user-a
                    :result/date    (biff/now)
                    :result/type :db.id/wod-result}
                   {:xt/id          :db.id/wod-result
                    :db/doc-type    :wod-result
                    :result/workout #uuid "4b4c75c1-56d5-49c7-b555-9747d54a16ea"
                    :result/scale   :rx}
                   {:db/doc-type :wod-set
                    :result-set/score 240
                    :result-set/parent :db.id/wod-result}])

  (let [{:keys [biff/db]} (get-context)]
    (xt/entity db #uuid "4b4c75c1-56d5-49c7-b555-9747d54a16ea"))

  (let [{:keys [biff/db]} (get-context)]
    (biff/q db '{:find  (pull r [* {:result-set/_result [*]}])
                 :where [[r :result/strength]]}))

  (def cindy (first (let [{:keys [biff/db]} (get-context)]
                      (biff/q db '{:find (pull w [*])
                                   :where [[w :workout/name "Cindy"]]}))))

  (biff/submit-tx (get-context)
                  [{:xt/id cindy
                    :db/doc-type :workout
                    :db/op :update
                    :workout/reps-per-round 30}])


  (let [{:keys [biff/db]} (get-context)]
    (biff/q db '{:find  (pull r [* {:result/type [* {:result-set/_parent [*]}]}])
                 :where [[r :result/user user-a]
                         [r :result/type rt]
                         [rt :result/workout]
                         [rs :result-set/parent rt]]}))

  (let [{:keys [biff/db]} (get-context)]
    (biff/q db '{:find  [(pull result [* {:result/type [*]}])]
                 :in    [[user-id workout-id]]
                 :where [[result :result/user user-id]
                         [result :result/type wod]
                         [wod :wod-result/workout workout-id]]}
            [user-a #uuid "c96c70c7-03ff-4825-8b31-ea69f3bd43a0"]))

  (get-results (get-context) user-a)



  (xt/entity (:biff/db (get-context)) #uuid "060bb19b-b243-4308-a4bd-5b5e71c7c8b8"))
