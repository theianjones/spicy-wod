(ns com.spicy.route-helpers)


(defn new-or-show
  [new-handler show-handler]
  (fn [request]
    (if (= "new" (-> request :path-params :id))
      (new-handler request)
      (show-handler request))))
