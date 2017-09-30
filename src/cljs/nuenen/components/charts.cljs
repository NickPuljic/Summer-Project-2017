(ns nuenen.components.charts
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
                          popover-anchor-wrapper border box]]
   [re-frame.core :as rf]
   [cljs.reader :as reader]
   [reagent.core :as r]
   [cljsjs.vega-lite]
   [cljsjs.vega-embed])
  (:import goog.History))

;; -------------------------
;; Charts Schema

(defn chart-schema [data title format x y c]
  {"$schema" "https://vega.github.io/schema/vega/v3.0.json"
   "width" 500
   "height" 200
   "padding" 15

   "data" [{ "name" "data"
            "values" data}]

   "scales" [{"name" "x"
              "type" "point"
              "range" "width"
              "domain" {"data" "data" "field" x}}
             {"name" "y"
              "type" "linear"
              "range" "height"
              "domain" {"data" "data" "field" y}}
             {"name" "color"
              "type" "ordinal"
              "range" "category"
              "domain" {"data" "data" "field" c}}]

   "title"{"text" title}

   "axes" [{"orient" "bottom"
            "scale" "x"
            "title" "Quarter"
            "ticks" true
            "grid" true
            "domain" false}
           {"orient" "top"
            "scale" "x"
            "ticks" false
            "grid" true
            "domain" false
            "labels" false}
           {"orient" "left"
            "scale" "y"
            "ticks" true
            "grid" true
            "domain" false
            "labels" true
            "format" format}
           {"orient" "right"
            "scale" "y"
            "ticks" false
            "grid" true
            "domain" false
            "labels" false}]

   "marks" [{"type" "group"
             "from" {"facet" {"name" "series"
                              "data" "data"
                              "groupby" c}}
             "marks" [{"type" "line"
                       "from" {"data" "series"}
                       "encode" {"enter" {"x" {"scale" "x" "field" x}
                                          "y" {"scale" "y" "field" y}
                                          "stroke" {"scale" "color" "field" c}
                                          "strokeWidth" {"value" 2}}
                                 "hover" { "fillOpacity" {"value" 1}}}}
                      {"type" "symbol"
                       "from" {"data" "series"}
                       "encode" {"enter" {"x" {"scale" "x" "field" x}
                                          "y" {"scale" "y" "field" y}
                                          "fill" {"scale" "color" "field" c}
                                          "stroke" {"scale" "color" "field" c}
                                          "strokeWidth" {"value" 1}
                                          "size" {"value" 30}}}}]}]})

;; -------------------------
;; JS Interlop

(defn log [a-thing]
  (.log js/console a-thing))

(defn parse-vega-spec [spec elem]
  (when spec
    (let [opts #js {"renderer" "canvas" "actions" false}]
      (js/vega.embed elem spec opts (fn [error res]
                                      (log error)
                                      (log res))))))

(defn vega
  "Reagent component that renders vega."
  [spec]
  (r/create-class
   {:display-name "vega"
    :component-did-mount (fn [this]
                           (parse-vega-spec spec (r/dom-node this)))
    :component-will-update (fn [this [_ new-spec]]
                             (parse-vega-spec new-spec (r/dom-node this)))
    :reagent-render (fn [spec]
                      [:div#vis])}))



;; -------------------------
;; Components
(defn ccn-selector-component
  []
  (let []
    [single-dropdown
     :choices @(rf/subscribe [:ccn-options])
     :model (rf/subscribe [:current-ccn])
     :width "100px"
     :filter-box? true
     :on-change #(rf/dispatch [:set-current-ccn %])]))

(defn charts-component
  []
  (let [graph-data (-> @(rf/subscribe [:snf-data]) :line-graphs)
        chart-data (->>
                    @(rf/subscribe [:snf-data])
                    :tour-survey
                    (filter #(= @(rf/subscribe [:current-ccn]) (:provider_ccn %)))
                    first)
        assessment-type (:assessment_type chart-data)
        qualitative-date (:qualitative_date chart-data)
        sorted-order {"Clinical Process And Services" 2
                      "Facility Assessment" 1
                      "Patient Ammenities" 4
                      "SNF Assessment Score" 5
                      "Staffing Assessment" 0
                      "Technology" 3}
        chart-formatted-data (->> (dissoc chart-data
                                          :assessment_type
                                          :qualitative_date
                                          :provider_ccn)
                                  (map (fn [[k v] r]
                                         (cond
                                           (= k :qualitative_score)
                                           (hash-map :category "SNF Assessment Score"
                                                     :standard-flags (str v "/36 Points")
                                                     :assessment-type (clojure.string/capitalize assessment-type)
                                                     :completion-date qualitative-date)
                                           :else
                                           (hash-map :category (->
                                                                k
                                                                name
                                                                (clojure.string/replace "_" " ")
                                                                (clojure.string/split #"\b")
                                                                (->> (map clojure.string/capitalize)
                                                                     (clojure.string/join)))
                                                     :standard-flags v))))
                                  (sort-by #(-> % :category sorted-order)))
        filter-one-ccn (fn [data]
                         (filter #(= @(rf/subscribe [:current-ccn]) (:post_acute_ccn %)) data))
        episode-volume-data (->> graph-data :epi_vol filter-one-ccn)
        SNF-CPE-data (->> graph-data :snf_cost filter-one-ccn) ; @elee, a semicolon denotes that there are more rows of data not being used currently
        SNF-LOS-data (->> graph-data :ilos filter-one-ccn) ;
        SNF-days-data (->> graph-data :snf_days filter-one-ccn) ;
        PER-data (->> graph-data :percent_read filter-one-ccn) ;
        RPE-data (->> graph-data :read_per_ep filter-one-ccn) ;
        STAR-data (->> graph-data :overall_star (filter #(= @(rf/subscribe [:current-ccn]) (:provider_ccn %))))
        short-STAR-data (->> graph-data :short_term_star (filter #(= @(rf/subscribe [:current-ccn]) (:provider_ccn %))))]
    [v-box
     :children [[gap :size "25px"]
                [h-box
                 :children [[gap :size "15px"]
                            [:h6 "Selected CCN"]]]
                [h-box
                 :children [[gap :size "15px"]
                            [ccn-selector-component]]]
                [gap :size "15px"]
                [h-box
                 :children [[gap :size "15px"]
                            (if-not (empty? episode-volume-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema episode-volume-data "Episode Volume" "" "anchor_beg_quarter" "episodes" "post_acute_ccn"))]])
                            [gap :size "15px"]
                            (if-not (empty? SNF-CPE-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema SNF-CPE-data "Avg SNF Cost per Episode" "$f" "anchor_beg_quarter" "sn_allowed" "c"))]])]]
                [gap :size "25px"]
                [h-box
                 :children [[gap :size "15px"]
                            (if-not (empty? SNF-LOS-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema SNF-LOS-data "Initial SNF LOS" ".1f" "anchor_beg_quarter" "first_stay_los_trimmed" "c"))]])
                            [gap :size "15px"]
                            (if-not (empty? SNF-days-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema SNF-days-data "SNF Days" "" "anchor_beg_quarter" "snf_days_trimmed" "c"))]])]]
                [gap :size "25px"]
                [h-box
                 :children [[gap :size "15px"]
                            (if-not (empty? PER-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema PER-data "% Episodes w a Readmit" "f" "anchor_beg_quarter" "readmission_flag" "c"))]])
                            [gap :size "15px"]
                            (if-not (empty? RPE-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema RPE-data "Readmits Per Episode" ".1f" "anchor_beg_quarter" "readmissions_count" "c"))]])]]
                [gap :size "25px"]
                [h-box
                 :children [[gap :size "15px"]
                            (if-not (empty? STAR-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema STAR-data "Overall STAR Rating" "" "month" "overall_rating" "c"))]])
                            [gap :size "15px"]
                            (if-not (empty? short-STAR-data)
                              [border
                               :border "2px solid black"
                               :child [vega (clj->js (chart-schema short-STAR-data "Short Term Adjusted STAR Rating" "" "month" "adjusted_st_overall_rating" "c"))]])]]
                [nuenen.components.tables/SNF-table nuenen.components.tables/snf-tour-columns chart-formatted-data "SNF Tour/Survey Summary" 55 65]]]))
