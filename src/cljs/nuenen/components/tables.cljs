(ns nuenen.components.tables
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
;; Formatters

(defn cents-formatter
  [x]
  (when x
    (if (and (> x -0.05)
             (neg? x))
      (str (gstring/format "%.1f%" (* -1 x)))
      (str (gstring/format "%.1f%" x)))))

(defn currency-formatter
  [x]
  (.format
   (goog.i18n.NumberFormat.
    (.-CURRENCY goog.i18n.NumberFormat.Format)) x))

(defn dollar-formatter
  [x]
  (when x
      (let [d (-> x
                  currency-formatter
                  (clojure.string/split ".")
                  first)]
        (if (neg? x)
          (str "(" (subs d 1) ")")
          d))))

(defn index-formatter
  [x]
  (-> x
      name
      (clojure.string/replace  "_" " ")
      (clojure.string/replace  "-" "/")
      clojure.string/capitalize))

(defn data-formatter
  [x]
  (-> x
      name
      (clojure.string/replace  "_" " ")
      (clojure.string/replace  "-" "/")
      (clojure.string/split #"\b")
      (->> (map clojure.string/capitalize)
           (clojure.string/join))))

;; -------------------------
;; Tables

(def Table (r/adapt-react-class js/FixedDataTable.Table))
(def Column (r/adapt-react-class js/FixedDataTable.Column))
(def ColumnGroup (r/adapt-react-class js/FixedDataTable.ColumnGroup))
(def Cell (r/adapt-react-class js/FixedDataTable.Cell))

(defn get-value [data index key]
  (-> data (nth index) (get key) str))

(defn header-cell [{:keys [title]}]
  (fn [props]
    (r/as-element [Cell title])))

(defn text-cell [{:keys [row-index field data]}]
  (fn [props]
    (let [index (.-rowIndex props)
          keyword-field (keyword field)
          value (get-value data index keyword-field)]
      (if (= "(" (first value))
        (r/as-element [:span {:style {:color "red"}} [Cell value]])
        (r/as-element [:span [Cell value]])))))

(defn format-table-data
  [data]
  (let [format-table (fn [formatter x]
                       (apply merge
                              (for [[k v] x]
                                (if (number? v)
                                  (hash-map k (formatter v))
                                  (hash-map k v)))))
        format-percent-table (partial format-table cents-formatter)
        format-absolute-table (partial format-table dollar-formatter)]
    (cond
      (and
       (= @(rf/subscribe [:restatement-tab]) :change-overview)
       (= @(rf/subscribe [:change-type]) "Percent")) (map format-percent-table data)
      :else (map format-absolute-table data))))

(defn index-column-group
  [columns data]
  [ColumnGroup {:fixed false
                :header (header-cell {:title ""})}
   (doall (for [c columns
                :let [field (:field c)
                      title (:title c)]
                :when (or (= title "Index") (= title "Metric") (= title "Bpid"))]
            ^{:key title} [Column {:cell (text-cell {:data data :field field})
                                   :header (header-cell {:title title})
                                   :width 100}]))])

(defn data-column-group
  [group-name search-on columns type data]
  ^{:key group-name} [ColumnGroup {:fixed false
                                   :header (header-cell {:title (if (= type "Change")
                                                                  group-name
                                                                  (clojure.string/upper-case group-name))})}
                      (doall (for [c columns
                                   :let [field (:field c)
                                         title (:title c)]
                                   :when (clojure.string/includes? title search-on)]
                               ^{:key title} [Column {:cell (text-cell {:data data :field field})
                                                      :header (header-cell {:title (if (= type "Change")
                                                                                     (clojure.string/upper-case (subs title (- (count title) 6)))
                                                                                     (subs title 0 (- (count title) 6)))})
                                                      :width 100}]))])

(defn restatement-table [columns type data]
  (let [filtered-data (if (:bpid (first data))
                        (filter #(contains? @(rf/subscribe [:bpid-selected]) (:bpid %)) data)
                        data)
        formatted-data (format-table-data filtered-data)
        items (if (= type "Change")
                (sort-by :id @(rf/subscribe [:change-items]))
                (sort-by :id @(rf/subscribe [:quarters-items])))
        rows-count (count formatted-data)
        row-height (if (= @(rf/subscribe [:restatement-tab]) :change-overview) 30 80)
        group-header-height 35
        header-height (if (= type "Change") 35 100)
        total-height (+ (+ header-height group-header-height) (* row-height rows-count) 1) ;;;;;;;;;;;;;;;;
        used-height (if (< total-height 695) total-height 695)
        overview? (-> columns first :field (= :index))]
    (when (> rows-count 0)
      [Table {:width (if overview? 700 800)
              :height used-height
              :rowHeight row-height
              :groupHeaderHeight group-header-height
              :headerHeight header-height
              :rowsCount rows-count}
       (index-column-group columns formatted-data)
       (doall (for [c items
                    :let [label (->> (clojure.string/split (:label c) #"\b")
                                     (map clojure.string/capitalize)
                                     (clojure.string/join))]]
                ^{:key label} (if (= type "Change")
                                (data-column-group label (subs label 12 31) columns "Change" formatted-data)
                                (data-column-group label (clojure.string/lower-case label) columns "Quarters" formatted-data))))])))

(def snf-tour-columns [{:field :category :title "Category"}
                       {:field :standard-flags :title "Standard Flags"}
                       {:field :assessment-type :title "Assessment Type"}
                       {:field :completion-date :title "Completion Date"}])

(def snf-columns [{:field :snf_name_concat :title "SNF"}
                  {:field :snf_tier :title "Hospital Network Tier*"}
                  {:field :null :title "Network Tier*"} ;
                  {:field :footprint_episodes_claims :title "Episode Initiator Volume**"}
                  {:field :live_remedy_episodes_claims :title "Volume**"}
                  {:field :NPRA_desc :title "NPRA per Episode"}
                  {:field :ilos_desc :title "Initial SNF LOS"}
                  {:field :readmit_desc :title "% Eps with a Readmission"}
                  {:field :adjusted_st_standard_sur :title "Short Term Adjusted Star Rating"}
                  {:field :qualitative_standard_sur :title "Tour or Survey"} ;
                  {:field :coop_score_standard_sur :title "Cooperation"}
                  {:field :status :title "Overall Status"}])

(defn SNF-header-cell [{:keys [title]}]
  (fn [props]
    (r/as-element [:span {:style {:align "center"}} [Cell title]])))

(defn SNF-text-cell [{:keys [row-index field data]}]
  (fn [props]
    (let [index (.-rowIndex props)
          keyword-field (keyword field)
          value (get-value data index keyword-field)]
      (cond
        (= value "P") (r/as-element [:span {:style {:color "Green" :font-size "20px" }} [:i.zmdi.zmdi-hc-fw-rc.zmdi-circle]])
        (= value "N") (r/as-element [:span {:style {:color "Yellow" :font-size "20px" }} [:i.zmdi.zmdi-hc-fw-rc.zmdi-circle]])
        (= value "U") (r/as-element [:span {:style {:color "Red" :font-size "20px" }} [:i.zmdi.zmdi-hc-fw-rc.zmdi-circle]])
        (= (count value) 13) (r/as-element [:span {:style {:font-size "12px" :align "center" :font-weight "500"}} [Cell value]])
        (= (count value) 1) (r/as-element [:span {:style {:font-size "12px" :align "center" :font-weight "500"}} [Cell value]])
        :else (r/as-element [:span {:style {:font-size "12px" :align "center"}} [Cell value]])))))

(defn SNF-column
  [column-name field data]
  ^{:key column-name}
  [Column {:cell (SNF-text-cell {:data data :field field})
           :header (SNF-header-cell {:title column-name})
           :width 100
           :align "center"}])

(defn SNF-table [columns data title header-height row-height]
  (let [rows-count (count data)
        columns-count (count columns)
        total-height (+ header-height (* row-height rows-count) 2) ;The two is so no scroll bar appears :)
        used-height (if (< total-height 695) total-height 695)
        overview? (-> columns first :field (= :index))]
    (when (> rows-count 0)
      [:div {:style {:margin "15px 3%"}}
       [:h5 title]
       [Table {:width (* columns-count 100)
               :height used-height
               :rowHeight row-height
               :headerHeight header-height
               :rowsCount rows-count}
        (doall (for [c columns
                     :let [title (:title c)
                           field (:field c)]]
                 ^{:key field} (SNF-column title field data)))]])))
