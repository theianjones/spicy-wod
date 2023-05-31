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


(comment

  ;; Call this in dev if you'd like to add some seed data to your database. If
  ;; you edit the seed data (in resources/fixtures.edn), you can reset the
  ;; database by running `rm -r storage/xtdb` (DON'T run that in prod),
  ;; restarting your app, and calling add-fixtures again.
  (add-fixtures "workouts.edn")
  (add-fixtures "movements.edn")

  ;; remove movements
  (let [{:keys [biff/db] :as ctx} (get-context)
        ids                       (q db
                                     '{:find  [e]
                                       :where [[e :movement/name]]})]
    (biff/submit-tx ctx (mapv (fn [[id]] {:db/op :delete
                                          :xt/id id}) ids)))

  (slurp (io/resource "movements.edn"))


  (biff/add-libs)

  (biff/submit-tx (get-context)
                  [{:db/doc-type   :movement
                    :movement/type :strength
                    :movement/name "Test"}])

  

  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find  (pull movement [*])
         :in    [[search]]
         :where [[movement :movement/name name]
                 [(string/includes? name search)]]}
       ["squat"]))

  (clojure.string/includes? "air squat" "squat")

  (require '[portal.api :as p])
  (def p (p/open))
  (add-tap #'p/submit)
  

  (let [{:keys [biff/db] :as ctx} (get-context)
        ids                       (q db
                                     '{:find  [e]
                                       :where [[e :xt/id]]})]
    (biff/submit-tx ctx (mapv (fn [[id]] {:db/op :delete
                                         :xt/id id}) ids)))

  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find  (pull result [* {:result/workout [*]}])
         :where [[result :result/notes]]}))

  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find  (pull result [* {:result/workout [*]}])
         :where [[user :user/email "user.b@example.com"]
                 [result :result/user user]]}))
  
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find  (pull workout [:workout/name :workout/scheme])
         :where [[workout :workout/name]]}))

  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find  (pull workout [*])
         :where [[workout :workout/name]]}))
  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db
       '{:find  (pull workout [*])
         :where [[workout :workout/name "Helen"]]}))
  (sort (keys (get-context)))

  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db '{:find  (pull workout [:workout/name])
            :in    [[user]]
            :where [(or [workout :workout/user user]
                        (and [workout :workout/name]
                             (not [workout :workout/user ])
                             ))]}
       [[#uuid "22dba77f-e2b1-42a1-bca6-9bbcf39e2625"]]))

  (let [{:keys [biff/db] :as ctx} (get-context)]
    #_(q db '{:find  (pull u [*])
              :in    [[user]]
              :where [[u :xt/id user]]}
         [["22dba77f-e2b1-42a1-bca6-9bbcf39e2625"]])
    (xt/entity db #uuid "22dba77f-e2b1-42a1-bca6-9bbcf39e2625"))



  (let [{:keys [biff/db] :as ctx} (get-context)]
    (q db '{:find  (pull workout [*])
            :where [[workout :workout/name]
                    (not [workout :workout/user u])]}))

  ;; Check the terminal for output.
  (biff/submit-job (get-context) :echo {:foo "bar"})
  (deref (biff/submit-job-for-result (get-context) :echo {:foo "bar"})))
