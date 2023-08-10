(ns com.spicy.ui
  (:require
    [cheshire.core :as cheshire]
    [clojure.java.io :as io]
    [clojure.string :as string]
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
                      :description (str settings/app-name " is a workout tracking app that builds a graph of your fitness progress over time connecting movements to workouts to results.")
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


(defn link
  [{:keys [match href class] :as attrs
    :or {class ""}}
   & children]
  (let [current-route? (or (= (:path match) href)
                           (= (:path match) (str href "/")))
        tag (if current-route? :div :a)
        current-route-class (when current-route? " bg-brand-background ")]
    [tag (merge attrs
                {:class    (str "text-black font-bold block p-4 text-lg border-b " class current-route-class)
                 :role     "menuitem"
                 :tabindex "-1"
                 :id       "menu-item-0"}) children]))


(defn page
  [{:keys [session reitit.core/match] :as ctx} & body]
  (base
    ctx
    [:.relative.h-full.p-4
     [:div {:class "absolute h-full -z-10 overflow-visible inset-0 bg-[url(/img/grid.svg)] bg-center "}]
     [:.p-3.mx-auto.max-w-screen-xl.w-full.flex.flex-col.sm:flex-row.gap-4.sm:justify-between.items-center.flex-wrap.space-y-2.justify-center
      [:a.flex.items-center.gap-2.cursor-pointer
       {:href "/"}
       [:img {:src "/img/spicywod-logo.png" :width 70 :height 70 :alt "spicy pepper"}]
       [:div {:class "font-display text-5xl w-[140px]"} "Spicy WOD"]]
      [:nav
       [:div {:class (str "relative text-left block sm:hidden ") :x-data "{ open: false }"}
        [:div
         [:button.btn {:type               "button"
                       :class              (str "flex items-center focus:ring-0 mx-auto ")
                       :id                 "menu-button"
                       :aria-expanded      "true"
                       :aria-haspopup      "true"
                       :x-on:click         "open = ! open"
                       :x-on:click.outside "open = false"}
          [:span.sr-only "Open options"]
          "Menu"]]
        [:div {:class            (str "absolute -right-16 z-10 mt-2 w-56 origin-top-right bg-white shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none ")
               :role             "menu"
               :aria-orientation "vertical"
               :aria-labelledby  "menu-button"
               :tabindex         "-1"
               :x-show           "open"}
         [:div {:role "none" :class (str "bg-white border-2 border-black ")}
          (link {:href "/app"
                 :match match} "Dashboard")
          (link {:href "/app/workouts"
                 :match match} "Workouts")
          (link {:href "/app/movements"
                 :match match} "Movements")
          (link {:href "/app/results"
                 :match match} "Scores")
          (when (:uid session)
            [:a {:href     "/app/results"
                 :class    (str "text-black font-bold block p-4 border-t text-lg  ")
                 :role     "menuitem"
                 :tabindex "-1"
                 :id       "menu-item-0"}
             (biff/form
               {:action "/auth/signout"
                :class  ""}
               [:button {:type  "submit"
                         :class (str "text-black font-bold block p-0 text-lg ")}
                "Sign out"])])]]]
       [:div.hidden.sm:flex.gap-4.pl-0.ml-0
        [:a.btn  {:href "/app/workouts"} "Workouts"]
        [:a.btn {:href "/app/movements"} "Movements"]
        [:a.btn {:href "/app/results"} "Scores"]
        (when (:uid session)
          [:div (biff/form
                  {:action "/auth/signout"
                   :class  "p-0 w-fit h-fit "}
                  [:button.btn {:type "submit"}
                   "Sign out"])])]]]
     [:.relative.sm:p-3.mx-auto.max-w-screen-xl.w-full
      (when (bound? #'csrf/*anti-forgery-token*)
        {:hx-headers (cheshire/generate-string
                       {:x-csrf-token csrf/*anti-forgery-token*})})

      body]
     [:.flex-grow]
     [:.flex-grow]]))

(defn share-page
  [{:keys [session reitit.core/match] :as ctx} & body]
  (base
   ctx
   [:.relative.h-full.px-4.pt-8
    [:div {:class "absolute bg-brand-background h-full -z-10 overflow-visible inset-0 bg-[url(/img/grid.svg)] bg-center "}]
    [:.p-3.mx-auto.max-w-screen-xl.w-full.flex.flex-col.sm:flex-row.gap-4.items-center.flex-wrap.space-y-2.justify-center
     [:a.flex.items-center.gap-2.cursor-pointer
      {:href "/"}
      [:img {:src "/img/spicywod-logo.png" :width 60 :height 60 :alt "spicy pepper"}]
      [:div {:class "font-display text-3xl w-[140px]"} "Spicy WOD"]]
     ]
    [:.relative.sm:p-3.mx-auto.max-w-screen-xl.w-full
     (when (bound? #'csrf/*anti-forgery-token*)
       {:hx-headers (cheshire/generate-string
                     {:x-csrf-token csrf/*anti-forgery-token*})})

     body]
    ]))


(defn panel
  [& children]
  [:div {:class (str  "bg-brand-background md:p-12 p-4 border-2 border-black "
                      " "
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
  [{:workout/keys [name description scheme] :keys [xt/id children class]}]
  [:a
   {:class (str
             "flex flex-col justify-between items-center gap-3 "
             "w-full sm:w-[354px] bg-white p-6 "
             "border-2 border-black hover:brutal-shadow"
             (or class ""))
    :href  (str "/app/workouts/" id)
    :title (str "View " name)
    :aria-label (str "View " name)}
   [:div.flex.items-center.justify-between.pb-2.sm:pb-4.w-full
    [:h2.text-3xl.cursor-default name]
    [:div.block.sm:hidden
     (when (some? children)
       children)]
    [:div.hidden.sm:block.border.border-radius.border-black.py-1.px-2.cursor-default.whitespace-nowrap.text-sm.self-start (display-scheme scheme)]]
   [:span.block.sm:hidden.cursor-default.w-fit.self-start.whitespace-nowrap (display-scheme scheme)]
   [:p.self-start.sm:self-center.whitespace-pre-wrap description]
   [:div.hidden.sm:block
    (when (some? children)
      children)]])
