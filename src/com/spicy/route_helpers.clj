(ns com.spicy.route-helpers)


(defn wildcard-override
  [default overrides]
  (fn [request]
    (let [route (-> request :path-params :id)
          handler (or ((keyword route) overrides) default)]
      (handler request))))


(defn ->key
  [n s]
  (keyword (str s "-" n)))


(defn htmx-request?
  [ctx]
  (-> ctx :headers
      (get "hx-request")
      (= "true")))


(comment
  (def handler (wildcard-override (constantly "default") {:new (constantly "new")}))
  (handler {:path-params {:id "some-id"}})
  (handler {:path-params {:id "new"}}))
