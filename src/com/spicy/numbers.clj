(ns com.spicy.numbers)


(defn parse-int
  [s]
  (Integer/parseInt (re-find #"\A-?\d+" s)))


(defn safe-parse-int
  [s]
  (try
    (parse-int s)
    (catch Exception e
      (prn "Error while parsing int: " e))))

