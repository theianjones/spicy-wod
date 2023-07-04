(ns com.spicy.time)


(defn ->time
  [seconds]
  (if (> 0 seconds)
    "00:00"
    (str (format "%02d" (quot seconds 60))
         ":"
         (format "%02d"  (rem seconds 60)))))
