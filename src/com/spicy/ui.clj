(ns com.spicy.ui
  (:require
    [cheshire.core :as cheshire]
    [clojure.java.io :as io]
    [com.biffweb :as biff]
    [com.spicy.settings :as settings]
    [ring.middleware.anti-forgery :as csrf]))


(defn css-path
  []
  (if-some [f (io/file (io/resource "public/css/main.css"))]
    (str "/css/main.css?t=" (.lastModified f))
    "/css/main.css"))


(defn base
  [{:keys [::recaptcha] :as ctx} & body]
  (apply
    biff/base-html
    (-> ctx
        (merge #:base{:title settings/app-name
                      :lang "en-US"
                      :icon "/img/glider.png"
                      :description (str settings/app-name " Description")
                      :image "https://clojure.org/images/clojure-logo-120b.png"})
        (update :base/head (fn [head]
                             (concat [[:link {:rel "stylesheet" :href (css-path)}]
                                      [:script {:src "https://unpkg.com/htmx.org@1.9.0"}]
                                      [:script {:src "https://unpkg.com/htmx.org/dist/ext/ws.js"}]
                                      [:script {:src "https://unpkg.com/hyperscript.org@0.9.8"}]
                                      (when recaptcha
                                        [:script {:src "https://www.google.com/recaptcha/api.js"
                                                  :async "async" :defer "defer"}])]
                                     head))))
    body))


(defn page
  [ctx & body]
  (base
    ctx
    [:nav
     [:ul.flex.list-none.gap-1
      [:li [:a {:href "/app/workouts"} "Workouts"]]
      [:li [:a {:href "/app/results"} "Results"]]]]
    [:.p-3.mx-auto.max-w-screen-sm.w-full
     (when (bound? #'csrf/*anti-forgery-token*)
       {:hx-headers (cheshire/generate-string
                      {:x-csrf-token csrf/*anti-forgery-token*})})
     body]
    [:.flex-grow]
    [:.flex-grow]))
