(ns com.spicy.middleware
  (:require
    [xtdb.api :as xt]))


(defn wrap-redirect-signed-in
  [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      {:status 303
       :headers {"location" "/app"}}
      (handler ctx))))


(defn wrap-signed-in
  [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:uid session))
      (handler ctx)
      {:status 303
       :headers {"location" "/signin?error=not-signed-in"}})))


(defn ensure-owner
  [user doc]
  (when (some #(or
                 (= user (get doc %))
                 (and
                   (nil? (get doc %))
                   (contains? (into #{} (keys doc)) :workout/name))) [:workout/user :result/user])
    doc))


(defn wrap-ensure-owner
  [handler]
  (fn [{:keys [biff/db session path-params params] :as req}]
    (let [entity-id (some-> (:id path-params)
                            parse-uuid)
          user      (:uid session)
          entity    (some->> entity-id
                             (xt/entity db)
                             (ensure-owner user))]
      (if (seq entity)
        (handler (merge req {:entity entity}))
        {:status 403}))))
