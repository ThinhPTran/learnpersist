(ns learnpersist.db
  (:require [hasch.core :refer [uuid]]
            [reagent.core :as r]
            [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect!
                                      subscribe-crdts!]]
            [cljs.core.async :refer [>! chan timeout]]
            [superv.async :refer [S] :as sasync]
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as s]
            [datascript.core :as d])
  (:require-macros [superv.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))

;; This part is for Datascript
;; Create a Datascript "connection" (an atom with the current DB value);
(defonce DBconn (d/create-conn))

;; This part is for share state and replikativ
(def user "trphthinh@gmail.com")
(def ormap-id #uuid "7d274663-9396-4247-910b-409ae35fe98d")
(def uri "ws://127.0.0.1:31744")

;;; Setup on client to communicate.
(declare client-state)
;; this is the only state changing function
(defn send-message! [app-store msg]
  (s/assoc! (:stage client-state)
            [user ormap-id]
            (uuid msg)
            [['assoc msg]]))
;;; Don't touch to the part above.

;; Have a look at the replikativ "Get started" tutorial to understand how the
;; replikativ parts work: http://replikativ.io/tut/get-started.html
(def stream-eval-fns
  {'assoc (fn [a new]
            (swap! a assoc (uuid new) new)
            a)
   'dissoc (fn [a new]
             (swap! a dissoc (uuid new))
             a)})

;; Datastore
(defonce app-store (atom (sorted-map)))
(defonce app-state (r/atom {:count 0
                            :datoms ["thinh" "dung"]}))

;; Setup sync state
(defn setup-replikativ []
  (go-try S
          (let [local-store (<? S (new-mem-store))
                local-peer (<? S (client-peer S local-store))
                stage (<? S (create-stage! user local-peer))
                stream (stream-into-identity! stage
                                              [user ormap-id]
                                              stream-eval-fns
                                              app-store)]
            (<? S (s/create-ormap! stage
                                   :description "messages"
                                   :id ormap-id))
            (connect! stage uri)
            {:store local-store
             :stage stage
             :stream stream
             :peer local-peer})))

(defn setupclientdata []
  (go-try S
          (def client-state (<? S (setup-replikativ)))))


