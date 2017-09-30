(ns nuenen.core
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-com.core   :refer [throbber single-dropdown]]
   [secretary.core :as secretary]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [nuenen.ajax :refer [load-interceptors!]]
   [nuenen.components.charts :as charts]
   [nuenen.components.tables]
   [nuenen.components.restatement]
   [nuenen.components.login-navbar]
   [nuenen.db]
   [nuenen.subscriptions]
   [cljsjs.vega-lite]
   [cljsjs.vega]
   [cljsjs.vega-embed])
  (:import goog.History))

;; -------------------------
;; Pages

(def report-type-list [{:id "Restatement Report" :label "Restatement Report"}
                       {:id "SNF Dashboard" :label "SNF Dashboard"}
                       {:id "SNF Drilldown" :label "SNF Drilldown"}])

(defn report-type-component
  []
  (let []
    [single-dropdown
     :choices report-type-list
     :model (rf/subscribe [:report-type])
     :width "300px"
     :style {:margin "0 3%"}
     :filter-box? true
     :on-change #(rf/dispatch [:set-report-type %])]))

(defn home-page
  []
  [:div
   [report-type-component]
   (if @(rf/subscribe [:login])
     (cond
       (= @(rf/subscribe [:report-type]) "Restatement Report") (if @(rf/subscribe [:table])
                                                                 [nuenen.components.restatement/tabs-and-table-componenet]
                                                                 [throbber :size :regular])
       (= @(rf/subscribe [:report-type]) "SNF Dashboard") (if @(rf/subscribe [:snf-data])
                                                            [nuenen.components.tables/SNF-table nuenen.components.tables/snf-columns (:snf-summary @(rf/subscribe [:snf-data])) "Performance Evaluation by SNF" 100 75]
                                                            [throbber :size :regular])
       (= @(rf/subscribe [:report-type]) "SNF Drilldown") [charts/charts-component])
     (set! (.-hash (.-location js/document)) "/login"))])

(defn login-page []
  [:div
   [:div.authpage]
   [nuenen.components.login-navbar/login-component]
   ])

(defn password-change-page []
  [nuenen.components.login-navbar/change-password-component])

(def pages
  {:home #'home-page
   :login-page #'login-page
   :changepassword #'password-change-page})

(defn page []
  [:div
   [nuenen.components.login-navbar/navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/login" []
  (rf/dispatch [:set-active-page :login-page]))

(secretary/defroute "/change-password" []
  (rf/dispatch [:set-active-page :changepassword]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     HistoryEventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app

(defn mount-components []
  (rf/clear-subscription-cache!)
  #_(nuenen.components.charts/parse-input)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:set-restatement-tab :change-overview])
  (rf/dispatch [:set-change-type "Absolute"])
  (rf/dispatch [:set-table-order "Quarter"])
  (rf/dispatch [:set-show-insights false])
  (rf/dispatch [:set-login false])
  (rf/dispatch [:set-current-system-id "439120"])
  (rf/dispatch [:set-show-bpid-filter false])
  (rf/dispatch [:set-report-type "Restatement Report"])
  (load-interceptors!)
  (hook-browser-navigation!)
  (mount-components))
