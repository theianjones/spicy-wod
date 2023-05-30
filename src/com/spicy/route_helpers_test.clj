(ns com.spicy.route-helpers-test
  (:require
    [clojure.test :as t]
    [com.spicy.route-helpers :as sut]))


(t/deftest wildcard-override
  (let [handler (sut/wildcard-override (constantly "default") {:override (constantly "override")})]

    (t/testing "returns default when override not found"
      (t/is (= "default" (handler {:path-params {:id "not-override"}}))))

    (t/testing "returns the override given the correct id"
      (t/is (= "override" (handler {:path-params {:id "override"}}))))))
