(ns com.spicy.results.score-test
  (:require
    [clojure.test :refer :all]
    [com.spicy.results.score :as sut]))


(def params-with-reps
  {:date            "2023-07-14"
   :notes-1         "fell off my pace"
   :scale           "rx"
   :reps-0          "116"
   :reps-2          "96"
   :reps-1          "102"
   :workout         "a02b0fe4-92f3-4044-af89-af028238b9f3"
   :notes-2         "couldnt think anymore"
   :notes           ""
   :scheme          :reps
   :rounds-to-score 3
   :notes-0         ""})


(def params-with-rounds-reps
  {:rounds-to-score 3
   :reps-per-round  100
   :date            "2023-07-14"
   :notes-1         "fell off my pace"
   :scale           "rx"
   :reps-0          "116"
   :rounds-0        "3"
   :reps-2          "96"
   :reps-1          "102"
   :rounds-2        "1"
   :rounds-1        "2"
   :workout         "a02b0fe4-92f3-4044-af89-af028238b9f3"
   :notes-2         "couldnt think anymore"
   :notes           ""
   :scheme          :rounds-reps
   :notes-0         ""})


(def params-with-minutes-seconds
  {:rounds-to-score 3
   :date            "2023-07-14"
   :notes-1         "fell off my pace"
   :scale           "rx"
   :minutes-0       "16"
   :seconds-0       "33"
   :minutes-1       "102"
   :seconds-1       "2"
   :minutes-2       "96"
   :seconds-2       "1"
   :workout         "a02b0fe4-92f3-4044-af89-af028238b9f3"
   :notes-2         "couldnt think anymore"
   :notes           ""
   :scheme          :time
   :notes-0         ""})


(deftest ->scores
  (testing "takes params with reps and outputs formated score"
    (is (= [{:reps "116" :index 0 :notes ""}
            {:reps "102" :index 1 :notes "fell off my pace"}
            {:reps "96" :index 2 :notes "couldnt think anymore"}]
           (sut/->scores params-with-reps))))
  (testing "defaults rounds to score to 1"
    (is (= [{:reps "116" :index 0 :notes ""}]
           (sut/->scores (dissoc params-with-reps :rounds-to-score)))))
  (testing "takes params with rounds and reps"
    (is (= [{:rounds "3" :index 0 :reps "116" :notes "" :reps-per-round 100}
            {:rounds         "2"
             :index          1
             :reps           "102"
             :notes          "fell off my pace"
             :reps-per-round 100}
            {:rounds         "1"
             :index          2
             :reps           "96"
             :notes          "couldnt think anymore"
             :reps-per-round 100}]
           (sut/->scores params-with-rounds-reps))))
  (testing "takes params with minutes seconds"
    (is (= [{:minutes "16" :index 0 :seconds "33" :notes ""}
            {:minutes "102" :index 1 :seconds "2" :notes "fell off my pace"}
            {:minutes "96"
             :index   2
             :seconds "1"
             :notes   "couldnt think anymore"}]
           (sut/->scores params-with-minutes-seconds)))))
