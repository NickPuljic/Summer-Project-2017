(ns nuenen.components.restatement
  (:require
   [ajax.core :refer [GET POST]]
   [cljsjs.fixed-data-table-2]
   [goog.events :as events]
   [goog.string :as gstring]
   [goog.string.format]
   [goog.i18n.NumberFormat :as nf]
   [goog.history.EventType :as HistoryEventType]
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

(defn insights-component
  []
  (let [show-insights @(rf/subscribe [:show-insights])
        table @(rf/subscribe [:table])
        insights (table :insights)]
    [:div {:style {:border "1px solid gray" :width "100%" :margin "0 0 5px" :padding "12px 13px 6px"}}
                 [:h6 "Summary "
                  [md-circle-icon-button
                   :md-icon-name (if show-insights "zmdi-minus" "zmdi-plus")
                   :size :smaller
                   :on-click #(rf/dispatch [:set-show-insights (not show-insights)])]]
                 [p
                  [:span
                   (-> insights :Overview first)]]
                 [:pre
                  (if (not show-insights)
                    {:style {:display "none"}})
                  (str
                   (-> insights second val first)
                   "\n\n"
                   (-> insights second val second))]
                 [p
                  [:span
                   (-> insights :Overview second)]]
                 [:pre
                  (if (not show-insights)
                    {:style {:display "none"}})
                  (str
                   (-> insights last val first)
                   "\n\n"
                   (-> insights last val second))]]))

(defn system-selector-component
  []
  (let [original-options @(rf/subscribe [:selector-options])
        options-map (into [] (map #(hash-map
                                    :id (str %2)
                                    :label (-> (clojure.string/replace (name %1) "-" " ")
                                               (clojure.string/split #"\b")
                                               (->> (map clojure.string/capitalize)
                                                    (clojure.string/join))))
                                  (keys original-options) (vals original-options)))
        selected-system-id (rf/subscribe [:current-system-id])]
    (fn []
      [single-dropdown
       :choices options-map
       :model selected-system-id
       :width "300px"
       :filter-box? true
       :on-change #(do
                     (rf/dispatch [:set-current-system-id %])
                     (rf/dispatch [:set-table nil])
                     (GET  (str "/restatement/" @selected-system-id) {:response-format :json
                                                                      :keywords? true
                                                                      :handler nuenen.request-handlers/table-handler}))])))

(defn quarter-selector-component
  []
  [selection-list
   :max-height "76px"
   :model (rf/subscribe [:quarters-selected])
   :choices (rf/subscribe [:quarters-items])
   :on-change #(rf/dispatch-sync [:set-quarters-selected %])])

(defn schema-selector-component
  []
  [selection-list
   :max-height "76px"
   :model (rf/subscribe [:change-selected])
   :choices (rf/subscribe [:change-items])
   :on-change #(rf/dispatch-sync [:set-change-selected %])])

(defn table-order-radio-button
  [{:keys [label value] :as order}]
  [radio-button
   :label label
   :label-style {:font-size "small"
                 :margin-top "2px"
                 :margin-bottom "2px"}
   :value value
   :model (rf/subscribe [:table-order])
   :on-change #(rf/dispatch [:set-table-order value])])

(defn table-order-selector-component
  []
  (let [orders [{:label "Quarter first, then Change"
                 :value "Quarter"}
                {:label "Change first, then Quarter"
                 :value "Change"}]]
    [v-box
     :style {:border "1px solid lightgrey"
             :border-radius "4px"}
     :class "rc-border display-flex"
     :children [[:div {:class "list-group noselect"
                       :style
                       {:margin-top "5px"
                        :padding "0px 5px"
                        :overflow-y "auto"
                        :margin-bottom "5px"}}
                 (doall (for [o orders]
                          ^{:key o} [:div {:class "list-group-item"}
                                     (table-order-radio-button o)]))]]]))

(defn type-of-change-radio-button
  [{:keys [label value]}]
  [radio-button
   :label label
   :label-style {:font-size "small"
                :margin-top "2px"
                :margin-bottom "2px"}
   :disabled? (if (= @(rf/subscribe [:restatement-tab]) :change-overview) false true)
   :value value
   :model (rf/subscribe [:change-type])
   :on-change #(rf/dispatch [:set-change-type value])])

(defn type-of-change-component
  []
  (let [type [{:label "Absolute Change"
               :value "Absolute"}
              {:label "% Change"
               :value "Percent"}]]
    [v-box
     :style {:border "1px solid lightgrey"
             :border-radius "4px"}
     :class "rc-border display-flex"
     :children [[:div {:class "list-group noselect"
                       :style
                       {:margin-top "5px"
                        :padding "0px 5px"
                        :overflow-y "auto"
                        :margin-bottom "5px"}}
                 (doall (for [t type]
                          ^{:key t} [:div {:class "list-group-item"}
                                     (type-of-change-radio-button t)]))]]]))

(defn bpid-filter-component
  []
  [popover-anchor-wrapper
   :showing? (rf/subscribe [:show-bpid-filter])
   :position :below-center
   :anchor [button
            :label (if (= false @(rf/subscribe [:show-bpid-filter]))
                     [:span "BPID" [:i.zmdi.zmdi-hc-fw-rc.zmdi-caret-right]]
                     [:span "BPID" [:i.zmdi.zmdi-hc-fw-rc.zmdi-caret-down]])
            :style {:color "black"
                    :font-size "14px"}
            :on-click #(rf/dispatch [:set-show-bpid-filter (not @(rf/subscribe [:show-bpid-filter]))])]
   :popover [popover-content-wrapper
             :no-clip? true
             :on-cancel #(rf/dispatch [:set-show-bpid-filter (not @(rf/subscribe [:show-bpid-filter]))])
             :body [scroller
                    :v-scroll :auto
                    :height "300px"
                    :child [selection-list
                            :choices (rf/subscribe [:bpid-options])
                            :model (rf/subscribe [:bpid-selected])
                            :on-change #(rf/dispatch [:set-bpids-selected %])]]]])

(defn table-tabs-component
  []
  (let [tabs [{:id :change-overview :label "Change Overview"}
              {:id :NPRA-change-factors :label "NPRA Change Factors"}]]
    [horizontal-tabs
     :model (rf/subscribe [:restatement-tab])
     :tabs tabs
     :on-change #(do
                   (rf/dispatch-sync [:set-restatement-tab %])
                   (if (= @(rf/subscribe [:restatement-tab]) :NPRA-change-factors)
                     (rf/dispatch-sync [:set-change-type "Absolute"])))]))


(defn filter-column-keys
  [original-keys]
  (let [quarters-selected (into [] @(rf/subscribe [:quarters-selected]))
        change-selected (into [] @(rf/subscribe [:change-selected]))]
    (into [] (mapcat (fn [change]
                   (mapcat (fn [quarter]
                             (filter #(and (clojure.string/includes? (name %) quarter)
                                          (clojure.string/includes? (name %) change))
                                     original-keys))
                           quarters-selected))
                 change-selected))))

(defn get-columns
  [column-keys]
  (for [k column-keys]
    (if (or
         (= (name k) "bpid")
         (= (name k) "metric")
         (= (name k) "index"))
      {:field k
       :title (nuenen.components.tables/index-formatter k)}
      {:field k
       :title (nuenen.components.tables/data-formatter k)})))

(defn make-columns
  [& args]
  (let [tab (if (= @(rf/subscribe [:restatement-tab]) :change-overview)
              (if (= @(rf/subscribe [:change-type]) "Absolute")
                [:overview-summary-absolute]
                [:overview-summary-percent])
              [:detail-summary])]
    (->> tab
         (get-in @(rf/subscribe [:table]))
         first
         keys
         (into [])
         (filter-column-keys)
         seq
         (#(apply conj % args))
         (get-columns)
         (sort-by :title))))

(defn filter-current-report
  []
  (let [selection-options @(rf/subscribe [:selector-options])]
    (->
     (filter (fn [x]
               (= (:id x) @(rf/subscribe [:current-system-id])))
             (into [] (map #(hash-map
                             :id (str %2)
                             :label (-> (clojure.string/replace (name %1) "-" " ")
                                        (clojure.string/split #"\b")
                                        (->> (map clojure.string/capitalize)
                                             (clojure.string/join))))
                           (keys selection-options) (vals selection-options))))
     first
     :label)))

(defn tabs-and-table-componenet
  []
  (let [change-items @(rf/subscribe [:change-items])
        change-selected @(rf/subscribe [:change-selected])
        quarters-items @(rf/subscribe [:quarters-items])
        quarters-selected @(rf/subscribe [:quarters-selected])
        system-id @(rf/subscribe [:current-system-id])
        table @(rf/subscribe [:table])
        tabs [{:id :change-overview :label "Change Overview"}
              {:id :NPRA-change-factors :label "NPRA Change Factors"}]
        overview-columns (make-columns :index)
        provider-columns (make-columns :metric :bpid)
        current-report [:h5 (str (filter-current-report)
                                 " Restatement Report")
                        [:a {:href (str "/restatement/download/" system-id)
                             :label "Download"}
                         [:i {:class "zmdi zmdi-download zmdi-hc-lg"
                              :style {:margin-left "11px"}}]]]]
    [v-box
     :style {:margin "0 3%"}
     :children [[gap :size "15px"]
                current-report
                [insights-component]
                [gap :size "25px"]
                [h-box
                 :children [[v-box
                             :children [(if @(rf/subscribe [:selector-options])
                                          [:h6 "Participant"])
                                        (if @(rf/subscribe [:selector-options])
                                          [system-selector-component])
                                        [gap :size "20px"]
                                        [:h6 "Schema Comparison"]
                                        [schema-selector-component]
                                        [gap :size "20px"]
                                        [:h6 "Quarter Comparison"]
                                        [quarter-selector-component]
                                        [gap :size "20px"]
                                        [:h6 "Table Order"]
                                        [table-order-selector-component]
                                        [gap :size "20px"]
                                        [:h6 "Type Of Change"]
                                        [type-of-change-component]
                                        [gap :size "20px"]]]
                            [gap :size "30px"]
                            [scroller
                             :v-scroll :auto
                             :child [v-box
                                     :max-width "775px"
                                     :align :start
                                     :children [[table-tabs-component]
                                                (if (= @(rf/subscribe [:table-order]) "Quarter")
                                                  [nuenen.components.tables/restatement-table overview-columns "Quarter" @(rf/subscribe [:overview-data])]
                                                  [nuenen.components.tables/restatement-table overview-columns "Change" @(rf/subscribe [:overview-data])])
                                                [gap :size "5px"]
                                                [bpid-filter-component]
                                                (if (= @(rf/subscribe [:table-order]) "Quarter")
                                                  [nuenen.components.tables/restatement-table provider-columns "Quarter" @(rf/subscribe [:provider-data])]
                                                  [nuenen.components.tables/restatement-table provider-columns "Change" @(rf/subscribe [:provider-data])])]]]]]]]))
