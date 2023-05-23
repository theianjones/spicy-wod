(ns com.spicy.app
  (:require
    [cheshire.core :as cheshire]
    [clojure.instant :as instant]
    [clojure.string :as string]
    [com.biffweb :as biff :refer [q]]
    [com.spicy.middleware :as mid]
    [com.spicy.settings :as settings]
    [com.spicy.ui :as ui]
    [ring.adapter.jetty9 :as jetty]
    [rum.core :as rum]
    [xtdb.api :as xt]))


(defn set-foo
  [{:keys [session params] :as ctx}]
  (biff/submit-tx ctx
                  [{:db/op :update
                    :db/doc-type :user
                    :xt/id (:uid session)
                    :user/foo (:foo params)}])
  {:status 303
   :headers {"location" "/app"}})


(defn bar-form
  [{:keys [value]}]
  (biff/form
    {:hx-post "/app/set-bar"
     :hx-swap "outerHTML"}
    [:label.block {:for "bar"} "Bar: "
     [:span.font-mono (pr-str value)]]
    [:.h-1]
    [:.flex
     [:input.w-full#bar {:type "text" :name "bar" :value value}]
     [:.w-3]
     [:button.btn {:type "submit"} "Update"]]
    [:.h-1]
    [:.text-sm.text-gray-600
     "This demonstrates updating a value with HTMX."]))


(defn set-bar
  [{:keys [session params] :as ctx}]
  (biff/submit-tx ctx
                  [{:db/op :update
                    :db/doc-type :user
                    :xt/id (:uid session)
                    :user/bar (:bar params)}])
  (biff/render (bar-form {:value (:bar params)})))


(defn message
  [{:msg/keys [text sent-at]}]
  [:.mt-3 {:_ "init send newMessage to #message-header"}
   [:.text-gray-600 (biff/format-date sent-at "dd MMM yyyy HH:mm:ss")]
   [:div text]])


(defn notify-clients
  [{:keys [com.spicy/chat-clients]} tx]
  (doseq [[op & args] (::xt/tx-ops tx)
          :when (= op ::xt/put)
          :let [[doc] args]
          :when (contains? doc :msg/text)
          :let [html (rum/render-static-markup
                       [:div#messages {:hx-swap-oob "afterbegin"}
                        (message doc)])]
          ws @chat-clients]
    (jetty/send! ws html)))


(defn send-message
  [{:keys [session] :as ctx} {:keys [text]}]
  (let [{:keys [text]} (cheshire/parse-string text true)]
    (biff/submit-tx ctx
                    [{:db/doc-type :msg
                      :msg/user (:uid session)
                      :msg/text text
                      :msg/sent-at :db/now}])))


(defn chat
  [{:keys [biff/db]}]
  (let [messages (q db
                    '{:find (pull msg [*])
                      :in [t0]
                      :where [[msg :msg/sent-at t]
                              [(<= t0 t)]]}
                    (biff/add-seconds (java.util.Date.) (* -60 10)))]
    [:div {:hx-ext "ws" :ws-connect "/app/chat"}
     [:form.mb-0 {:ws-send true
                  :_ "on submit set value of #message to ''"}
      [:label.block {:for "message"} "Write a message"]
      [:.h-1]
      [:textarea.w-full#message {:name "text"}]
      [:.h-1]
      [:.text-sm.text-gray-600
       "Sign in with an incognito window to have a conversation with yourself."]
      [:.h-2]
      [:div [:button.btn {:type "submit"} "Send message"]]]
     [:.h-6]
     [:div#message-header
      {:_ "on newMessage put 'Messages sent in the past 10 minutes:' into me"}
      (if (empty? messages)
        "No messages yet."
        "Messages sent in the past 10 minutes:")]
     [:div#messages
      (map message (sort-by :msg/sent-at #(compare %2 %1) messages))]]))


(defn panel
  [& children]
  [:div {:class (str  "rounded-3xl bg-brand-pink md:p-12 p-4 "
                      "drop-shadow-[2px_2px_0px_rgba(0,0,0,100)] "
                      "flex flex-col item-center ")}
   children])


(defn workout-results
  [{:keys [user workout biff/db]}]
  (let [results (biff/q db '{:find (pull result [*])
                             :in [[user-id workout-id]]
                             :where [[result :result/user user-id]
                                     [result :result/workout workout-id]]}
                        [user workout])]
    [:div {:class (str "flex flex-col relative h-full md:min-h-[60vh] p-8 rounded-md shadow-[-2px_-2px_0px_rgba(0,0,0,100)]")}
     [:div {:class "absolute h-full rounded-md  bg-brand-background shadow-[-2px_-2px_0px_rgba(0,0,0,100)] -z-10 overflow-visible inset-0 bg-[url(/img/grid.svg)] bg-center "}]
     [:h2.text-3xl "Log Book"]
     (if (zero? (count results))
       [:p {:class (str " w-fit m-auto ")} "Log a workout to see your history!"]
       [:ul.list-none.list-inside.gap-3.pl-0.ml-0
        (map (fn [{:result/keys [score date notes scale]}]
               [:li {:class (str "w-1/2")}
                [:div.flex.gap-3.flex-col
                 [:.flex.justify-between.flex-wrap.gap-2
                  [:div.text-2xl.font-bold.self-center score 
                   [:span.pl-2.font-normal (name scale)]]
                  [:div.self-center (biff/format-date
                                     date "EEE, YYYY-MM-dd")]]
                 (when notes [:div notes])
                 ]]) results)])]))


(defn display-scheme
  [scheme]
  (case scheme
    :time        "For Time"
    :rounds-reps "Rounds + Reps"
    :pass-fail   "Pass/Fail"
    nil))


(defn workout-ui
  [{:workout/keys [name description scheme] :keys [children class]}]
  [:div
   {:class (str
             "flex flex-col items-center gap-3 "
             "w-[354px] bg-brand-background p-6 rounded-md "
             "drop-shadow-[2px_2px_0px_rgba(0,0,0,100)] "
             (or class ""))}
   [:div.flex.items-center.justify-between.pb-4.w-full
    [:h2.text-3xl.cursor-default name]
    [:div.border.border-radius.rounded-full.border-black.py-1.px-2.cursor-default (display-scheme scheme)]]
   [:p description]
   (when (some? children)
     children)])


(defn index-workout
  [{:keys [biff/db session] :as _ctx}]
  (prn (:uid session))
  (let [workouts (biff/q db '{:find (pull workout [*])
                              :in [[user]]
                              :where [(or (and [workout :workout/name]
                                               (not [workout :workout/user]))
                                          [workout :workout/user user])]}
                         [(:uid session)])]
    (ui/page {}
             (panel
               [:h1.text-5xl.mb-14 "Workouts"]
               [:div.flex.gap-4.flex-wrap.justify-center
                (map #(workout-ui
                        (assoc %
                               :children
                               [:a.brutal-shadow.btn-hover.border.border-radius.rounded.border-black.py-1.px-2.mt-auto
                                {:href (str "/app/workouts/" (string/lower-case (:workout/name %)))}
                                "History"]))
                     workouts)]))))


(defn workout-logbook-ui
  [{:workout/keys [name description scheme] :keys [children class]}]
  [:div
   {:class (str
            "flex flex-col items-center gap-3 "
            "w-[354px] p-8 pb-0 sm:pb-6 "
            (or class ""))}
   [:div.flex.flex-col.w-full
    [:h2.text-3xl.cursor-default name]
    [:div.py-1.cursor-default (display-scheme scheme)]]
   [:p description]
   (when (some? children)
     children)])

(defn show-workout
  [{:keys [biff/db path-params session params] :as _ctx}]
  (let [workout (first (biff/q db '{:find  (pull workout [*])
                                    :in    [[name id]]
                                    :where [(or [workout :workout/name name]
                                                [workout :xt/id id])]}
                               [(string/capitalize (:id path-params))
                                (parse-uuid (:id path-params))]))]
    (if (true? (:fragment params))
      (workout-ui workout)
      (ui/page {} (panel [:div {:class (str "h-full flex flex-col gap-6 md:flex-row")}
                          [:div.flex.gap-2.flex-col
                           (workout-logbook-ui workout)
                           [:a {:href  (str "/app/results/new?workout=" (:workout/name workout))
                                :class (str "btn h-fit w-fit place-self-center")} "Log workout"]]
                          [:.flex-1
                           (workout-results
                             {:user    (:uid session)
                              :workout (:xt/id workout)
                              :biff/db db})]])))))


(defn workout-form
  [{:keys [hidden]}]
  (biff/form
    {:class "flex flex-col gap-4"
     :action "/app/workouts"
     :hidden hidden}
    [:input.pink-input.teal-focus#name {:placeholder "Name" :name "name"}]
    [:textarea.pink-input.teal-focus#description {:placeholder "Description" :name "description"}]
    [:select.pink-input.teal-focus#scheme {:name "scheme"}
     [:option {:value "" :label "--Select a Workout Scheme--"}]
     [:option {:value "time"
               :label "time"}]
     [:option {:value "time-with-cap"
               :label "time-with-cap"}]
     [:option {:value "pass-fail"
               :label "pass-fail"}]
     [:option {:value "rounds-reps"
               :label "rounds-reps"}]
     [:option {:value "reps"
               :label "reps"}]
     [:option {:value "emom"
               :label "emom"}]
     [:option {:value "load"
               :label "load"}]
     [:option {:value "calories"
               :label "calories"}]
     [:option {:value "meters"
               :label "meters"}]
     [:option {:value "feet"
               :label "feet"}]
     [:option {:value "points"
               :label "points"}]]
    [:button.btn {:type "submit"} "Create Workout"]))


(defn new-workout
  [{:keys [biff/db path-params session params] :as _ctx}]
  (let [fragment? (:fragment params)]
    (if fragment?
      (workout-form {:hidden {:fragment (str (true? fragment?))}})
      (ui/page {}
               (panel
                 [:h1.text-5xl.mb-14 "New Workout"]
                 (workout-form {:hidden {:fragment (str (true? fragment?))}}))))))


(defn create-workout
  [{:keys [biff/db path-params session params] :as ctx}]
  (let [workout-uuid (random-uuid)]
    (biff/submit-tx ctx [{:db/op               :merge
                          :db/doc-type         :workout
                          :xt/id               workout-uuid
                          :workout/name        (:name params)
                          :workout/user        (:uid session)
                          :workout/scheme      (keyword (:scheme params))
                          :workout/description (:description params)}])
    {:status  303
     :headers {"location" (str "/app/workouts/" workout-uuid "?fragment=" (true?  (:fragment params)))}}))


(defn result-ui
  [{:result/keys [score workout] :as result}]
  [:div#result-ui
   [:div
    [:a {:href (str "/app/workouts/" (string/lower-case (:workout/name workout)))} (:workout/name workout)]]
   [:div score]
   [:button.btn {:hx-get (str "/app/results/" (:xt/id result) "/edit")
                 :hx-target "closest #result-ui"} "Edit"]])


(defn scheme-forms
  [{:workout/keys [scheme _secondary-scheme] :result/keys [score]}]
  (case scheme
    :time (let [[minutes seconds] (string/split (or score ":") #":")]
            [:div.flex.gap-3
             [:input.w-full.pink-input.teal-focus#minutes
              {:type        "text"
               :name        "minutes"
               :placeholder "Minutes"
               :value       minutes}]
             [:input.w-full.pink-input.teal-focus#seconds
              {:type        "text"
               :name        "seconds"
               :placeholder "Seconds"
               :value       seconds}]])
    :time-with-cap "todo"
    :pass-fail [:input.w-full#reps
                {:type "text" :name "reps" :placeholder "Rounds Successfully Completed" :value score}]
    :rounds-reps
    (let [[rounds reps] (string/split (or score "+") #"\+")]
      [:div.flex.gap-3
       [:input.w-full.pink-input.teal-focus#rounds
        {:type "text" :name "rounds" :placeholder "Rounds" :value rounds}]
       [:input.w-full.pink-input.teal-focus#reps
        {:type "text" :name "reps" :placeholder "Reps" :value reps}]])
    [:input.w-full#reps {:type "text" :name "reps" :placeholder "Reps" :value score}]))


(defn result-form
  [{:keys [workout result action hidden hx-key] :as props} & children]
  (biff/form
    {(or hx-key :action) action
     :class              "flex flex-col gap-3"
     :hidden             hidden}
    (scheme-forms (merge workout result))
    [:input.pink-input.teal-focus
     {:type  "date"
      :name  "date"
      :value (biff/format-date
               (or (:result/date result) (biff/now)) "YYYY-MM-dd")}]
    [:div.flex.gap-2.items-center
     [:div.flex-1.flex.gap-2.items-center
      [:input#rx {:type    "radio"
                  :name    "scale"
                  :value   "rx"
                  :checked (= (:result/scale result) :rx)}]
      [:label {:for "rx"} "Rx"]]
     [:div.flex-1.flex.gap-2.items-center
      [:input#scaled {:type    "radio"
                      :name    "scale"
                      :value   "scaled"
                      :checked (= (:result/scale result) :scaled)}]
      [:label {:for "scaled"} "Scaled"]]
     [:div.flex-1.flex.gap-2.items-center
      [:input#rx+ {:type    "radio"
                   :name    "scale"
                   :value   "rx+"
                   :checked (= (:result/scale result) :rx+)}]
      [:label {:for "rx+"} "Rx+"]]]
    [:textarea.w-full.pink-input.teal-focus#notes {:name        "notes"
                                                   :placeholder "notes"
                                                   :value       (:result/notes result)}]
    children))


(defn params->score
  [{:keys [minutes seconds rounds reps]}]
  (cond
    (or (some? minutes) (some? seconds)) (str minutes ":" seconds)
    (and (some? rounds) (some? reps)) (str rounds "+" reps)
    (some? reps) reps
    :else nil))


(defn show-result
  [{:keys [biff/db session path-params]}]
  (let [result (first (biff/q db '{:find (pull result [* {:result/workout [*]}])
                                   :in [[result-id user]]
                                   :where [[result :xt/id result-id]
                                           [result :result/user user]]}
                              [(parse-uuid (:id path-params)) (:uid session)]))]
    (result-ui result)))


(defn update-result
  [{:keys [biff/db params session path-params] :as ctx}]

  (let [result (first (biff/q db '{:find (pull result [* {:result/workout [*]}])
                                   :in [[result-id user]]
                                   :where [[result :xt/id result-id]
                                           [result :result/user user]]}
                              [(parse-uuid (:id path-params)) (:uid session)]))
        tx (biff/submit-tx ctx
                           [{:db/op :update
                             :db/doc-type :result
                             :xt/id (parse-uuid (:id path-params))
                             :result/user (:uid session)
                             :result/score (params->score params)
                             :result/notes (:notes params)
                             :result/scale (keyword (:scale params))
                             :result/date (instant/read-instant-date (:date params))}])]
    {:status 303
     :headers {"Location" (str "/app/results/" (:id path-params))}}))


(defn edit-result
  [{:keys [biff/db session path-params]}]
  (let [{:result/keys [workout] :as result}
        (first (biff/q db '{:find (pull result [* {:result/workout [*]}])
                            :in [[result-id user]]
                            :where [[result :xt/id result-id]
                                    [result :result/user user]]}
                       [(parse-uuid (:id path-params)) (:uid session)]))
        result-path (str "/app/results/" (:xt/id result))]
    [:div#edit-result
     (result-form
       (assoc {} :result result :workout workout :action result-path :hx-key :hx-put)
       [:button.btn {:type "submit"} "Update Result"]
       [:button.btn.bg-red-400.hover:bg-red-600 {:hx-get result-path
                                                 :hx-target "closest #edit-result"} "Cancel"])]))


(defn index-result
  [{:keys [biff/db session] :as _ctx}]
  (let [results (biff/q db '{:find (pull result [* {:result/workout [:workout/name]}])
                             :in [[user]]
                             :where [[result :result/user user]]}
                        [(:uid session)])]
    (ui/page {} [:div
                 [:h1 "Results"]
                 [:ul
                  (map (fn [r]
                         (let [workout-name (get-in r [:result/workout :workout/name])]
                           [:li
                            (result-ui r)])) results)]])))


(defn create-result
  [{:keys [biff/db session params] :as ctx}]
  (biff/submit-tx ctx
                  [{:db/op :merge
                    :db/doc-type :result
                    :result/workout (parse-uuid (:workout params))
                    :result/user (:uid session)
                    :result/score (params->score params)
                    :result/notes (:notes params)
                    :result/scale (keyword (:scale params))
                    :result/date (instant/read-instant-date (:date params))}])
  (let [{:workout/keys [name] :as w} (xt/entity db (parse-uuid (:workout params)))]
    {:status 303
     :headers {"location" (str "/app/workouts/" (string/lower-case name))}}))


(defn new-result
  [{:keys [biff/db session params] :as ctx}]
  (let [workout (first (biff/q db
                               '{:find (pull workout [*])
                                 :in [[name]]
                                 :where [[workout :workout/name name]]}
                               [(:workout params)]))]

    (ui/page {} (panel
                 [:div {:class (str "md:min-h-[60vh] ")}
                 [:h1 {:class (str "text-5xl mb-14 ")} "Log Result"]
                 [:.flex.flex-col.gap-6.md:flex-row
                  (workout-ui workout)
                  [:.flex-1
                   (result-form
                    (merge {:workout workout} {:action "/app/results" :hidden {:workout (:xt/id workout)}})
                    [:button.btn {:type "submit"} "Log Result"])]]]))))


(defn app
  [{:keys [session biff/db] :as ctx}]
  (let [{:user/keys [email foo bar]} (xt/entity db (:uid session))]
    (ui/page
      {}
      [:div "Signed in as " email ". "
       (biff/form
         {:action "/auth/signout"
          :class "inline"}
         [:button.text-black.hover:text-brand-pink.font-bold {:type "submit"}
          "Sign out"])
       "."]
      [:.h-6]
      (biff/form
        {:action "/app/set-foo"}
        [:label.block {:for "foo"} "Foo: "
         [:span.font-mono (pr-str foo)]]
        [:.h-1]
        [:.flex
         [:input.w-full#foo {:type "text" :name "foo" :value foo}]
         [:.w-3]
         [:button.btn {:type "submit"} "Update"]]
        [:.h-1]
        [:.text-sm.text-gray-600
         "This demonstrates updating a value with a plain old form."])
      [:.h-6]
      (bar-form {:value bar})
      [:.h-6]
      (chat ctx))))


(defn ws-handler
  [{:keys [com.spicy/chat-clients] :as ctx}]
  {:status 101
   :headers {"upgrade" "websocket"
             "connection" "upgrade"}
   :ws {:on-connect (fn [ws]
                      (swap! chat-clients conj ws))
        :on-text (fn [ws text-message]
                   (send-message ctx {:ws ws :text text-message}))
        :on-close (fn [ws status-code reason]
                    (swap! chat-clients disj ws))}})


(def about-page
  (ui/page
    {:base/title (str "About " settings/app-name)}
    [:p "This app was made with "
     [:a.link {:href "https://biffweb.com"} "Biff"] "."]))


(defn echo
  [{:keys [params]}]
  {:status 200
   :headers {"content-type" "application/json"}
   :body params})


(defn new-or-show
  [new-handler show-handler]
  (fn [request]
    (prn (-> request :path-params))
    (if (= "new" (-> request :path-params :id))
      (new-handler request)
      (show-handler request))))


(def plugin
  {:static {"/about/" about-page}
   :routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["" {:get app}]
            ["/workouts"
             ["" {:get index-workout
                  :post create-workout}]
             ["/:id" {:get (new-or-show new-workout show-workout)}]]
            ["/results"
             ["" {:get index-result
                  :post create-result}]
             ["/:id"
              ["" {:get (new-or-show new-result show-result)
                   :put update-result}]
              ["/edit" {:get edit-result}]]]
            ["/set-foo" {:post set-foo}]
            ["/set-bar" {:post set-bar}]
            ["/chat" {:get ws-handler}]]
   :api-routes [["/api/echo" {:post echo}]]
   :on-tx notify-clients})
