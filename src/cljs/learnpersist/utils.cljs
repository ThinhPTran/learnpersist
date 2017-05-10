(ns learnpersist.utils)

(defn create-msg [id att value]
  {:id id
   :att att
   :val value
   :inst (.getTime (js/Date.))})

(defn get-datoms [conn]
  (mapv #(zipmap [:e :a :v :t :add] %) (:eavt @conn)))