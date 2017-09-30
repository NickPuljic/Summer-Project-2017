(ns nuenen.subscriptions
  (:require [re-frame.core :refer [reg-sub]]
            [re-frame.core :as rf]))

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
 :login
 (fn [db _]
   (:login db)))

(reg-sub
 :table
 (fn [db _]
   (:table db)))

(reg-sub
 :restatement-tab
 (fn [db _]
   (:restatement-tab db)))

(reg-sub
 :overview-data
 (fn [db _]
   (if (= @(rf/subscribe [:restatement-tab]) :change-overview)
     (if (= @(rf/subscribe [:change-type]) "Absolute")
       (get-in db [:table :overview-summary-absolute])
       (get-in db [:table :overview-summary-percent]))
     (get-in db [:table :detail-summary]))))

(reg-sub
 :provider-data
 (fn [db _]
   (if (= @(rf/subscribe [:restatement-tab]) :change-overview)
     (if (= @(rf/subscribe [:change-type]) "Absolute")
       (get-in db [:table :overview-provider-absolute])
       (get-in db [:table :overview-provider-percent]))
     (get-in db [:table :detail-provider]))))

(reg-sub
 :show-insights
 (fn [db _]
   (:show-insights db)))

(reg-sub
 :table-order
 (fn [db _]
   (:table-order db)))

(reg-sub
 :change-items
 (fn [db _]
   (:change-items db)))

(reg-sub
 :change-selected
 (fn [db _]
   (:change-selected db)))

(reg-sub
 :quarters-items
 (fn [db _]
   (:quarters-items db)))

(reg-sub
 :quarters-selected
 (fn [db _]
   (:quarters-selected db)))

(reg-sub
 :selector-options
 (fn [db _]
   (:selector-options db)))

(reg-sub
 :current-system-id
 (fn [db _]
   (:current-system-id db)))

(reg-sub
 :change-type
 (fn [db _]
   (:change-type db)))

(reg-sub
 :bpid-options
 (fn [db _]
   (:bpid-options db)))

(reg-sub
 :bpid-selected
 (fn [db _]
   (:bpid-selected db)))

(reg-sub
 :show-bpid-filter
 (fn [db _]
   (:show-bpid-filter db)))

(reg-sub
 :report-type
 (fn [db _]
   (:report-type db)))

(reg-sub
 :snf-data
 (fn [db _]
   (:snf-data db)))

(reg-sub
 :ccn-options
 (fn [db _]
   (:ccn-options db)))

(reg-sub
 :current-ccn
 (fn [db _]
   (:current-ccn db)))

(reg-sub
 :file
 (fn [db _]
   (:file db)))

(reg-sub
 :upload-status
 (fn [db _]
   (:upload-status db)))
