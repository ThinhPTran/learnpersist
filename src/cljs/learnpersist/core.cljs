(ns learnpersist.core
  (:require
   [reagent.core :as r]
   [learnpersist.db :as mydb]
   [learnpersist.events :as events]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars

(defonce app-state (r/atom {}))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page

(defn userdatoms []
  (let [datoms (:datoms @mydb/app-state)]
    [:div
     [:input
      {:type "button"
       :value "Add datom"
       :on-click (fn [_]
                   (events/addDatom))}]
     [:div "Datoms: "]
     [:ul
      (for [datom datoms]
        ^{:key datom} [:li (str datom)])]]))


(defn usercount []
  (let [count (:count @mydb/app-state)]
    [:div
     [:div.row
      [:div "Count: "]
      [:div (str count)]]
     [:input
      {:type "button"
       :value "Add"
       :on-click (fn [_]
                   (events/addCount))}]]))

(defn page [ratom]
  [:div "Welcome to reagent-figwheel."
   [usercount]
   [userdatoms]])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App

(defn dev-setup []
  (when ^boolean js/goog.DEBUG
    (enable-console-print!)
    (println "dev mode")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Reload & Main

(defn reload []
  (r/render [page app-state]
            (.getElementById js/document "app")))

(defn ^:export main []
  (dev-setup)
  (reload)
  (mydb/setupclientdata))
