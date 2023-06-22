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
               ;; :in    [[user]]
               ;; :where [[result :result/user user]]
               :where [[result :result/type]]}))


(comment
  ;; Call this in dev if you'd like to add some seed data to your database. If
  ;; you edit the seed data (in resources/fixtures.edn), you can reset the
  ;; database by running `rm -r storage/xtdb` (DON'T run that in prod),
  ;; restarting your app, and calling add-fixtures again.
  (add-fixtures "workouts.edn")
  (add-fixtures "movements.edn")

  (def user-a #uuid "cd92c14b-f20b-4b75-97c9-5ade5c0d80e9")
  (def mu-movement #uuid "078726e4-1225-40e9-a48a-edb38d38dfa8")

  (biff/submit-tx (get-context)
                  [{:xt/id       :db.id/result
                    :db/doc-type :result
                    :db/op       :create
                    :result/user user-a
                    :result/type :db.id/stength-result}
                   {:xt/id             :db.id/wod-result
                    :db/doc-type       :strength-result
                    :result/movement   mu-movement
                    :result/set-count  3
                    }
                   {:db/doc-type       :strength-set
                    :result-set/result :db.id/result
                    :result-set/status :pass
                    :result-set/reps   5
                    :result-set/weight 20}
                   {:db/doc-type       :strength-set
                    :result-set/result :db.id/result
                    :result-set/status :pass
                    :result-set/reps   5
                    :result-set/weight 15}
                   {:db/doc-type       :strength-set
                    :result-set/result :db.id/result
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
                    :result/type :db.id/wod-result}
                   {:xt/id          :db.id/wod-result
                    :db/doc-type    :wod-result
                    :result/workout #uuid "c96c70c7-03ff-4825-8b31-ea69f3bd43a0"
                    :result/score   "4:00"
                    :result/date    (biff/now)
                    :result/scale   :rx}])

  (let [{:keys [biff/db]} (get-context)]
    (biff/q db '{:find  (pull r [* {:result-set/_result [*]}])
                 :where [[r :result/strength]]}))

  (let [{:keys [biff/db]} (get-context)]
    (biff/q db '{:find  [(pull result [* {:result/type [*]}])]
                 :in    [[user-id workout-id]]
                 :where [[result :result/user user-id]
                         [result :result/type wod]
                         [wod :wod-result/workout workout-id]]}
            [user-a #uuid "c96c70c7-03ff-4825-8b31-ea69f3bd43a0"]))

  (get-results (get-context) user-a)



  (xt/entity (:biff/db (get-context)) #uuid "060bb19b-b243-4308-a4bd-5b5e71c7c8b8"))
