(ns com.spicy.portal
  "Interact with Portal, used to navigate through your data.
   https://github.com/djblue/portal"
  (:require
    [clojure.datafy :as d]
    [portal.api :as p]))


(def portal
  "Holds the Portal atom which holds the current selection in the Splash Taps UI.
   To get the value of the current selection, it needs to be deref'd twice `@@portal`"
  (atom nil))


(defn- submit
  "Datafy exceptions before sending to Portal"
  [value]
  (p/submit
    (if (instance? Exception value)
      (d/datafy value)
      value)))


(defn open-portal
  "Opens the Portal window and listens to `tap>`"
  ([] (open-portal {}))
  ([opts]
   (add-tap #'submit)
   (reset! portal (p/open (merge {:window-title "Splash Taps"} opts)))))


(comment
  ;; Opens Portal in a separate window
  (open-portal)

  ;; Opens Portal in the integrated extension
  ;; Documentation at https://github.com/djblue/portal/tree/master/doc/editors
  (open-portal {:launcher :vs-code})
  (open-portal {:launcher :intellij})
  (open-portal {:launcher :emacs})

  ;; Opens Portal in a separate window but connected to your editor
  (open-portal {:editor :vs-code})

  ;; Get the value of the selection in Portal
  @@portal
  )


(ns com.spicy.portal)
