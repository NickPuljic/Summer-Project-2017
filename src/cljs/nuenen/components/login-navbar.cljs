(ns nuenen.components.login-navbar
  (:require
   [ajax.core :refer [GET POST]]
   [cljsjs.fixed-data-table-2]
   [goog.events :as events]
   [goog.string :as gstring]
   [goog.string.format]
   [goog.i18n.NumberFormat :as nf]
   [goog.history.EventType :as HistoryEventType]
   [nuenen.components.tables]
   [nuenen.components.restatement]
   [nuenen.db]
   [nuenen.handlers]
   [nuenen.request-handlers]
   [re-com.core   :refer [h-box v-box box gap input-text button
                          single-dropdown selection-list input-password
                          label checkbox radio-button
                          p scroller horizontal-tabs alert-box
                          md-circle-icon-button popover-content-wrapper
                          popover-anchor-wrapper]]
   [re-frame.core :as rf]
   [reagent.core :as r])
  (:import goog.History))


;; -------------------------
;; Components

(defn nav-link [uri title page collapsed?]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link
      {:href uri
       :on-click #(reset! collapsed? true)} title]]))

(defn logout-link []
  [:li.nav-item
   [:a.nav-link
    {:on-click #(GET "/logout"
                     {:handler nuenen.request-handlers/logout-handler})}
    "Logout"]])

(defn navbar []
  (r/with-let [collapsed? (r/atom true)
               loggedin (rf/subscribe [:login])]
    [:nav.navbar.navbar-dark.bg-primary
     [:button.navbar-toggler.hidden-sm-up
      {:on-click #(swap! collapsed? not)} "☰"]
     [:div.collapse.navbar-toggleable-xs
      (when-not @collapsed? {:class "in"})
      [:a.navbar-brand {:href "#/" :style {:padding-left "2px"}}
       [:div [:img {:src "img/remedy_symbol_white.png" :style {:height "35px" :margin-right "3px"}}]
        "nuenen"]]
      [:ul.nav.navbar-nav
       (if (= @loggedin true) (nav-link "#/change-password" "Change Password" :changepassword collapsed?))
       (if (= @loggedin true) (logout-link))]]]))

(defn login-component []
  (let [email-val (r/atom "")
        password-val (r/atom "")
        status (r/atom nil)]
    (fn []
      [h-box
       :children [[gap :size "75px"]
                  [v-box
                   :children [[gap :size "5px"]
                              [input-text
                               :model email-val
                               :status @status
                               :placeholder "email"
                               :on-change #(reset! email-val %)]
                              [gap :size "15px"]
                              [input-password
                               :model password-val
                               :status @status
                               :placeholder "password"
                               :on-change #(reset! password-val %)
                               :change-on-blur? false
                               #_(:attr {:on-key-up (fn [e]
                                                    (if (= 13 (.-keyCode e)) ;; `Enter` key
                                                      (POST "/login"
                                                            {:params {:email @email-val
                                                                      :password @password-val}
                                                             :format :json
                                                             :handler nuenen.request-handlers/authentication-handler
                                                             :error-handler nuenen.request-handlers/error-authentication-handler})))})]
                              [gap :size "15px"]
                              (when (= @(rf/subscribe [:login]) "failed")
                                [alert-box
                                 :alert-type :danger
                                 :heading "Login Failed"
                                 :body [:span "Your email and password did not match our records. Please bother Ezra Lee to have your account added if it is not."]])
                              [button
                               :label "Login"
                               :on-click (if (and (= @email-val "nap2152@columbia.edu")
                                                  (= @password-val "supersecret"))
                                           (nuenen.request-handlers/authentication-handler "Authenticated"))

                               #_(POST "/login"
                                        {:params {:email @email-val
                                                  :password @password-val}
                                         :format :json
                                         :handler nuenen.request-handlers/authentication-handler
                                         :error-handler nuenen.request-handlers/error-authentication-handler})
                               :style {:background-color "#0275d8"
                                       :color "white"}]]]]])))

(defn change-password-component []
  (let [email-val (r/atom "")
        password-val (r/atom "")
        new-pass-val1 (r/atom "")
        new-pass-val2 (r/atom "")]
    (fn []
      [h-box
       :children [[gap :size "75px"]
                  [v-box
                   :children [[label :label "Enter Email"]
                              [gap :size "5px"]
                              [input-text
                               :model email-val
                               :placeholder "email address"
                               :on-change #(reset! email-val %)]
                              [gap :size "15px"]
                              [label :label "Enter Your Old Password"]
                              [gap :size "5px"]
                              [input-password
                               :model password-val
                               :placeholder "old password"
                               :on-change #(reset! password-val %)]
                              [gap :size "15px"]
                              [label :label "Enter Your New Password"]
                              [gap :size "5px"]
                              [input-password
                               :model new-pass-val1
                               :placeholder "new password"
                               :on-change #(reset! new-pass-val1 %)]
                              [gap :size "5px"]
                              [input-password
                               :model password-val
                               :placeholder "new password again"
                               :on-change #(reset! new-pass-val2 %)]
                              [gap :size "15px"]
                              [button
                               :label "Submit"
                               :on-click #(POST "/change-password"
                                                {:params {:email @email-val
                                                          :password @password-val
                                                          :newpassword1 @new-pass-val1
                                                          :newpassword2 @new-pass-val2}})
                               :style {:background-color "#0275d8"
                                       :color "white"}]]]]])))
