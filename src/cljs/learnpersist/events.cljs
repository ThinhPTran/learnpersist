(ns learnpersist.events
  (:require [clojure.string :as str]
            [learnpersist.db :as mydb :refer [send-message! app-store app-state DBconn]]
            [learnpersist.utils :as utils :refer [create-msg]]
            [datascript.core :as d]))


;; Events
(defn addCount []
  (swap! app-state update-in [:count] inc)
  (send-message! app-store (create-msg -1 :inc 1)))

(defn addDatom []
  (let [datom [{:db/id -1 :name (str "Name " (rand-int 1000)) :age (rand-int 100)}]]
    (d/transact! DBconn datom)))

;; Handler for App-store
(defn app-store-handle-changes []
  (let [allactions (vals @app-store)]
    (.log js/console "handle-app-store-handle-changes: " (str allactions))
    (swap! app-state assoc :datoms (vec allactions))))

(add-watch app-store :key #(app-store-handle-changes))

;; Handler for DBconn
(defn DBconn-handle-changes []
  (let [datoms (utils/get-datoms DBconn)]
    (.log js/console "datoms: " (str datoms))
    (doseq [datom datoms]
      (send-message! app-store (create-msg (:e datom) (:a datom) (:v datom))))))

(add-watch DBconn :key #(DBconn-handle-changes))
