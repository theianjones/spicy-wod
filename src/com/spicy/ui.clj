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
                             (concat [[:link {:rel "preload" :href "/fonts/DDSans-Regular.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "preload" :href "/fonts/DDSans-Light.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "preload" :href "/fonts/DDSans-Bold.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "preload" :href "/fonts/Mazer.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "stylesheet" :href (css-path)}]
                                      [:script {:src "https://unpkg.com/htmx.org@1.9.0"}]
                                      [:script {:src "https://unpkg.com/htmx.org/dist/ext/ws.js"}]
                                      [:script {:src "https://unpkg.com/hyperscript.org@0.9.8"}]
                                      (when recaptcha
                                        [:script {:src "https://www.google.com/recaptcha/api.js"
                                                  :async "async" :defer "defer"}])]
                                     head))))
    body))


(defn page
  [{:keys [session] :as ctx} & body]
  (base
   ctx
   [:.relative.h-full
    [:div {:class "absolute h-full -z-10 overflow-visible inset-0 bg-[url(/img/grid.svg)] bg-center "}]
    [:.p-3.mx-auto.max-w-screen-xl.w-full.flex.sm:justify-between.items-center.flex-wrap.space-y-2.justify-center
     [:a.flex.items-center.gap-2.cursor-pointer
      {:href "/"}
      [:img {:src "/img/spicywod-logo.png" :width 70 :height 70 :alt "spicy pepper"}]
      [:div {:class "font-display text-5xl w-[140px]"} "Spicy WOD"]]
     [:nav
      [:ul.flex.list-none.list-inside.gap-3.pl-0.ml-0
       [:li [:a.btn {:href "/app/workouts"} "Workouts"]]
       [:li [:a.btn {:href "/app/results"} "Scores"]]
       (when (:uid session) [:button "logout"])
       ]]] 
    [:.relative.sm:p-3.mx-auto.max-w-screen-xl.w-full
     (when (bound? #'csrf/*anti-forgery-token*)
       {:hx-headers (cheshire/generate-string
                     {:x-csrf-token csrf/*anti-forgery-token*})})

     body]
    [:.flex-grow]
    [:.flex-grow]]))
