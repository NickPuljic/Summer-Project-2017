(ns nuenen.handlers
  (:require [nuenen.db :as db]
            [re-frame.core :refer [dispatch reg-event-db]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (println "PAGE" page)
    (assoc db :page page)))

(reg-event-db
 :set-login
 (fn [db [_ login]]
   (assoc db :login login)))

(reg-event-db
 :set-table
 (fn [db [_ table]]
   (assoc db :table table)))

(reg-event-db
 :set-restatement-tab
 (fn [db [_ tab]]
   (assoc db :restatement-tab tab)))

(reg-event-db
 :set-change-items
 (fn [db [_ items]]
   (assoc db :change-items items)))

(reg-event-db
 :set-change-selected
 (fn [db [_ selected]]
   (assoc db :change-selected selected)))

(reg-event-db
 :set-quarters-items
 (fn [db [_ items]]
   (assoc db :quarters-items items)))

(reg-event-db
 :set-quarters-selected
 (fn [db [_ selected]]
   (assoc db :quarters-selected selected)))

(reg-event-db
 :set-selector-options
 (fn [db [_ options]]
   (assoc db :selector-options options)))

(reg-event-db
 :set-show-insights
 (fn [db [_ show]]
   (assoc db :show-insights show)))

(reg-event-db
 :set-table-order
 (fn [db [_ order]]
   (assoc db :table-order order)))

(reg-event-db
 :set-current-system-id
 (fn [db [_ id]]
   (assoc db :current-system-id id)))

(reg-event-db
 :set-change-type
 (fn [db [_ type]]
   (assoc db :change-type type)))

(reg-event-db
 :set-bpids-options
 (fn [db [_ bpid-options]]
   (assoc db :bpid-options bpid-options)))

(reg-event-db
 :set-bpids-selected
 (fn [db [_ bpid-selected]]
   (assoc db :bpid-selected bpid-selected)))

(reg-event-db
 :set-show-bpid-filter
 (fn [db [_ filter]]
   (assoc db :show-bpid-filter filter)))

(reg-event-db
 :set-report-type
 (fn [db [_ report]]
   (assoc db :report-type report)))

(reg-event-db
 :set-snf-data
 (fn [db [_ snf-data]]
   (assoc db :snf-data snf-data)))

(reg-event-db
 :set-ccn-options
 (fn [db [_ options]]
   (assoc db :ccn-options options)))

(reg-event-db
 :set-current-ccn
 (fn [db [_ ccn]]
   (assoc db :current-ccn ccn)))

(reg-event-db
 :save-rm-file
 (fn [db [_ file]]
   (assoc db :file file))
 )

(reg-event-db
 :set-upload-status
 (fn [db [_ upload-success]]
   (assoc db :upload-status upload-success)))
