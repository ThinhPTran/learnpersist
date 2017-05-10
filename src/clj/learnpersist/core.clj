(ns learnpersist.core
  (:require [hasch.core :refer [uuid]]
            [replikativ.peer :refer [server-peer client-peer]]
            [kabel.peer :refer [start stop]]
            [replikativ.stage :refer [create-stage! connect!
                                      subscribe-crdts!]]
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as s]
            [jdbc.core :as jdbc]
            [konserve.memory :refer [new-mem-store]]
            [superv.async :refer [<?? <? S go-try]] ;; core.async error handling
            [clojure.core.async :refer [chan] :as async]))

;; -------------------------
;; Test Firebird2.5
(def dns {:description "Tao2 Firebird Database"
          :classname   "org.firebirdsql.jdbc.FBDriver"
          :subprotocol "firebirdsql"
          :subname     "//localhost:3050//home/setup/databases/test.fdb"
          :user "glueuser"
          :password "glue"})

;(with-open [FDBconn (jdbc/connection dns)]
;  (println "About to create a table!!!")
;  (jdbc/execute FDBconn "CREATE TABLE datoms (id integer, att varchar(500), val varchar(500), inst varchar(500));"))

;; This part is for share state
(def user "trphthinh@gmail.com")
(def ormap-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")
(def uri "ws://127.0.0.1:31744")

;; Have a look at the replikativ "Get started" tutorial to understand how the
;; replikativ parts work: http://replikativ.io/tut/get-started.html
(def stream-eval-fns
  {'assoc (fn [a new]
            (swap! a assoc (uuid new) new)
            a)
   'dissoc (fn [a new]
             (swap! a dissoc (uuid new))
             a)})

(def app-store (atom (sorted-map)))

;; Handler
(add-watch app-store :watcher
           (fn [key atom old-state new-state]
             (let [allactions (vals new-state)
                   conn (jdbc/connection dns)]
               (println "allactions: " allactions)
               (doseq [action allactions]
                 (jdbc/execute conn ["INSERT INTO datoms VALUES (?,?,?,?);" (:id action) (str (:att action)) (str (:val action)) (str (:inst action))])))))

(defn -main [& args]
  (let [store (<?? S (new-mem-store))
        peer (<?? S (server-peer S store uri))
        stage (<?? S (create-stage! user peer))
        stream (stream-into-identity! stage
                                      [user ormap-id]
                                      stream-eval-fns
                                      app-store)]
    (<?? S (s/create-ormap! stage
                           :description "messages"
                           :id ormap-id))
    (connect! stage uri)
    (<?? S (start peer))
    (println "Replikativ server clone!" uri)
    ;; HACK blocking main termination
    (<?? S (chan))))