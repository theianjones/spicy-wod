

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
        (merge #:base{:title (str settings/app-name " | A workout tracking app you can trust")
                      :lang "en-US"
                      :icon "/img/favicon-32x32.png"
                      :description (str settings/app-name "is a workout tracking app that builds a graph of your fitness progress over time connecting movements to workouts to results.")
                      :image "/img/spicy-og-banner.png"})
        (update :base/head (fn [head]
                             (concat [[:link {:rel "preload" :href "/fonts/DDSans-Regular.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "preload" :href "/fonts/DDSans-Light.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "preload" :href "/fonts/DDSans-Bold.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "preload" :href "/fonts/Mazer.otf" :as "font" :type "font/otf" :crossorigin "anonymous"}]
                                      [:link {:rel "stylesheet" :href (css-path)}]
                                      [:script {:src "/js/alpinejs_3_12_1.js" :defer true}]
                                      [:script {:src "/js/htmx_1_9_2.js" :defer true}]
                                      [:link {:rel "apple-touch-icon" :sizes "180x180" :href "/img/apple-touch-icon.png"}]
                                      [:link {:rel "icon" :type "image/png" :sizes "32x32" :href "/img/favicon-32x32.png"}]
                                      [:link {:rel "icon" :type "image/png" :sizes "16x16" :href "/img/favicon-16x16.png"}]
                                      [:link {:rel "manifest" :href "/img/site.webmanifest"}]
                                      [:link {:rel "mask-icon" :href "/img/safari-pinned-tab.svg" :color "#0c0001"}]
                                      [:meta {:name "msapplication-TileColor" :content "#ffeef0"}]
                                      [:meta {:name "theme-color" :content "#ffffff"}]
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
        [:li [:a.btn {:href "/app/movements"} "Movements"]]
        [:li [:a.btn {:href "/app/results"} "Scores"]]
        (when (:uid session)
          (biff/form
            {:action "/auth/signout"
             :class "inline"}
            [:button.text-blue-500.hover:text-blue-800 {:type "submit"}
             "Sign out"]))]]]
     [:.relative.sm:p-3.mx-auto.max-w-screen-xl.w-full
      (when (bound? #'csrf/*anti-forgery-token*)
        {:hx-headers (cheshire/generate-string
                       {:x-csrf-token csrf/*anti-forgery-token*})})

      body]
     [:.flex-grow]
     [:.flex-grow]]))


(defn panel
  [& children]
  [:div {:class (str  "rounded-3xl bg-brand-pink md:p-12 p-4 "
                      "drop-shadow-[2px_2px_0px_rgba(0,0,0,100)] "
                      "flex flex-col item-center ")}
   children])


(defn display-scheme
  [scheme]
  (case scheme
    :time        "For Time"
    :time-with-cap "For Time"
    :rounds-reps "Rounds + Reps"
    :reps        "Total Reps"
    :pass-fail   "Pass/Fail"
    :load        "Total Load"
    :calories    "Total Cals"
    :meters      "Total Meters"
    :feet        "Total Feet"
    :points      "Points"
    :emom        "EMOM"
    nil))


(defn workout-ui
  [{:workout/keys [name description scheme] :keys [children class]}]
  [:div
   {:class (str
             "flex flex-col justify-between items-center gap-3 "
             "w-full sm:w-[354px] bg-brand-background p-6 sm:rounded-md "
             "drop-shadow-[2px_2px_0px_rgba(0,0,0,100)] "
             (or class ""))}
   [:div.flex.items-center.justify-between.pb-2.sm:pb-4.w-full
    [:h2.text-3xl.cursor-default name]
    [:div.block.sm:hidden
     (when (some? children)
       children)]
    [:div.hidden.sm:block.border.border-radius.rounded-full.border-black.py-1.px-2.cursor-default.whitespace-nowrap.text-sm.self-start (display-scheme scheme)]]
   [:span.block.sm:hidden.cursor-default.w-fit.self-start.whitespace-nowrap (display-scheme scheme)]
   [:p.self-start.sm:self-center.whitespace-pre-wrap description]
   [:div.hidden.sm:block
    (when (some? children)
      children)]])
