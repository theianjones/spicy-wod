(ns com.spicy.time-test
  (:require
    [clojure.test :as t]
    [com.spicy.time :as sut]))


(t/deftest ->time
  (t/testing "returns 00:00 for times less than 0"
    (t/is (= "00:00"
             (sut/->time 0)))
    (t/is (= "00:00"
             (sut/->time -1))))
  (t/testing "pads minutes"
    (t/is (= "01:00"
             (sut/->time 60)))
    (t/is (= "10:00"
             (sut/->time 600))))
  (t/testing "pads seconds"
    (t/is (= "01:01"
             (sut/->time 61)))
    (t/is (= "10:10"
             (sut/->time 610)))))
