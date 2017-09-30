(ns nuenen.request-handlers
  (:require
   [ajax.core :refer [GET POST]]
   [camel-snake-kebab.core :as csk]
   [medley.core :refer [map-keys]]
   [nuenen.db]
   [nuenen.handlers]
   [nuenen.subscriptions]
   [re-frame.core :as rf]
   ))

;; -------------------------
;; Handler Functions

(defn rectify-data
  [{:keys [columns index data] :as table}]
  (let [named-rows (map (fn [r]
                          (->> (map
                                #(sorted-map (-> (str (first %) "_" (second %))
                                                 (clojure.string/replace " " "_")
                                                 (clojure.string/replace "/" "-")
                                                 keyword) %2)
                                columns r)
                               (apply merge)
                               ))
                        data)]
    (if (= (count (first index)) 2)
      (do
        (rf/dispatch [:set-bpids-options (map #(hash-map :label % :id %)
                                              (sort (set (map #(first %)
                                                              index))))])
        (rf/dispatch [:set-bpids-selected (set (map #(first %)
                                                    index))])
        (map #(assoc %2 :metric (second %) :bpid (first %))
             index named-rows))
      (map #(assoc %2 :index %)
           index named-rows))))

(defn snf-rectify-data
  [{:keys [columns data] :as table}]
  (let [named-rows (map (fn [r]
                          (->> (map
                                #(sorted-map (keyword %) %2)
                                columns r)
                               (apply merge)
                               ))
                        data)]
    named-rows))

(defn set-change-quarters-options
  [overview]
  (let [formatted-change (sort (map #(-> (str (first %))
                                          (clojure.string/replace "% " "")
                                          (clojure.string/replace " " "_")
                                          (clojure.string/replace "/" "-")) overview))
        change-selected (-> formatted-change set)
        change-options (into []
                             (map #(hash-map
                                    :id %
                                    :label (-> %
                                               (clojure.string/replace "-" "/")
                                               (clojure.string/replace "_" " "))) (distinct formatted-change)))
        formatted-quarters (sort (map #(str (second %)) overview))
        quarters-selected (-> formatted-quarters set)
        quarters-options (into []
                                (map #(hash-map
                                       :id %
                                       :label %) (distinct formatted-quarters)))]
    (rf/dispatch [:set-change-selected change-selected])
    (rf/dispatch [:set-change-items change-options])
    (rf/dispatch [:set-quarters-selected quarters-selected])
    (rf/dispatch [:set-quarters-items quarters-options])))

;; -------------------------
;; Handlers

(defn table-handler
  [response]
  (let [new-response (map-keys csk/->kebab-case-keyword response)
        cols (->> new-response keys (remove #{:insights}))
        table (reduce #(update %1 %2 rectify-data) new-response cols)]
    (set-change-quarters-options (:columns (:overview-summary-percent new-response)))
    (rf/dispatch [:set-table table])
    )
  (println "Handled"))

(defn snf-handler
  [response]
  (let [new-response (map-keys csk/->kebab-case-keyword response)
        cols (->> new-response keys (remove #{:insights :line-graphs}))
        snf-table (reduce #(update %1 %2 snf-rectify-data) new-response cols)
        graph-cols (-> snf-table :line-graphs keys)
        snf-table-w-graphs (->>
                            (reduce #(update %1 %2 snf-rectify-data)
                                    (:line-graphs snf-table) graph-cols)
                            (assoc snf-table :line-graphs))
        ccn-options (map
                     #(hash-map
                       :id (subs (:snf_name_concat %) 0 6)
                       :label (subs (:snf_name_concat %) 0 6))
                     (:snf-summary snf-table-w-graphs))]
    (rf/dispatch [:set-ccn-options ccn-options])
    (rf/dispatch [:set-current-ccn  (:label (first ccn-options))])
    (rf/dispatch [:set-snf-data snf-table-w-graphs])
    )
  (println "SNF-handled"))

(defn system-selector-handler
  [response]
  (let [new-response (map-keys csk/->kebab-case-keyword response)]
    (rf/dispatch [:set-selector-options new-response])))

(defn error-authentication-handler
  [response]
  (rf/dispatch [:set-login "failed"])
  (println (:status response) "failure"))

(defn authentication-handler
  [response]
  (if (= response "Authenticated")
    (do
      (rf/dispatch [:set-login true])
      (set! (.-hash (.-location js/document)) "/")
      (rf/dispatch [:set-active-page :home])
      (GET  "/participants" {:response-format :json
                             :keywords? true
                             :handler system-selector-handler})
      (GET  "/restatement/1" {:response-format :json
                              :keywords? true
                              :handler table-handler})
      (GET "/snfnew" {:response-format :json
                      :keywords? true
                      :handler snf-handler}))))

(defn logout-handler
  [response]
  (rf/dispatch [:set-login false])
  (set! (.-hash (.-location js/document)) "/")
  (rf/dispatch [:set-active-page :home]))
