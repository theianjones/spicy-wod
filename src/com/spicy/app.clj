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
  [:div {:class (str  "rounded-3xl bg-brand-pink md:p-8 p-4 "
                      "drop-shadow-[2px_2px_0px_rgba(0,0,0,100)] "
                      "flex flex-col item-center")}
   children])


(defn workout-results
  [{:keys [user workout biff/db]}]
  (let [results (biff/q db '{:find (pull result [*])
                             :in [[user-id workout-id]]
                             :where [[result :result/user user-id]
                                     [result :result/workout workout-id]]}
                        [user workout])]
    [:div
     [:h2.text-2xl "Log Book"]
     (if (zero? (count results))
       [:p "Log a workout to see your history!"]
       [:ul
        (map (fn [{:result/keys [score date notes]}]
               [:li
                [:div.flex.gap-3
                 [:div score]
                 (when notes [:div notes])
                 [:div date]]]) results)])]))


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
    [:h2.text-3xl name]
    [:div.border.border-radius.rounded-full.border-black.py-1.px-2 (display-scheme scheme)]]
   [:p description]
   (when (some? children)
     children)])


(defn index-workout
  [{:keys [biff/db] :as _ctx}]
  (let [workouts (biff/q db '{:find  (pull workout [*])
                              :where [[workout :workout/name]]})]
    (ui/page {}
             (panel
               [:h1.text-5xl.mb-14 "Workouts"]
               [:div.flex.gap-4.flex-wrap.justify-center
                (map #(workout-ui
                        (assoc %
                               :children
                               [:a.btn.mt-auto
                                {:href (str "/app/workouts/" (string/lower-case (:workout/name %)))}
                                "View History"]))
                     workouts)]))))


(defn show-workout
  [{:keys [biff/db path-params session] :as _ctx}]
  (let [workout (first (biff/q db '{:find  (pull workout [*])
                                    :in    [[name]]
                                    :where [[workout :workout/name name]]}
                               [(string/capitalize (:name path-params))]))]
    (ui/page {} (panel [:div.flex.flex-col.gap-6.md:flex-row
                        [:div.flex.gap-2.md:block
                         (workout-ui workout)
                         [:.h-6]
                         [:a {:href  (str "/app/results/new?workout=" (:workout/name workout))
                              :class (str "btn h-[fit-content]")} "Log workout"]]
                        [:.flex-1
                         (workout-results {:user (:uid session) :workout (:xt/id workout) :biff/db db})]]))))


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
                  [:h1.text-5xl.mb-14 "Log Result"]
                  [:.flex.flex-col.gap-6.md:flex-row
                   (workout-ui workout)
                   [:.flex-1
                    (result-form
                      (merge {:workout workout} {:action "/app/results" :hidden {:workout (:xt/id workout)}})
                      [:button.btn {:type "submit"} "Log Result"])]]))))


(defn app
  [{:keys [session biff/db] :as ctx}]
  (let [{:user/keys [email foo bar]} (xt/entity db (:uid session))]
    (ui/page
      {}
      [:div "Signed in as " email ". "
       (biff/form
         {:action "/auth/signout"
          :class "inline"}
         [:button.text-blue-500.hover:text-blue-800 {:type "submit"}
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


(def plugin
  {:static {"/about/" about-page}
   :routes ["/app" {:middleware [mid/wrap-signed-in]}
            ["" {:get app}]
            ["/workouts"
             ["" {:get index-workout}]
             ["/:name" {:get show-workout}]]
            ["/results"
             ["" {:get index-result
                  :post create-result}]
             ["/:id"
              ["" {:get
                   (fn [request]
                     (if (= "new" (-> request :path-params :id))
                       (new-result request)
                       (show-result request)))
                   :put update-result}]
              ["/edit" {:get edit-result}]]]
            ["/set-foo" {:post set-foo}]
            ["/set-bar" {:post set-bar}]
            ["/chat" {:get ws-handler}]]
   :api-routes [["/api/echo" {:post echo}]]
   :on-tx notify-clients})
