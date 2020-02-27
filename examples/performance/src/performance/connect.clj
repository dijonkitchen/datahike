(ns performance.connect
  (:require [performance.measure :refer [measure-connect-times]]
            [performance.uri :as uri]
            [performance.db :as db]
            [performance.const :as c]
            [incanter.io]
            [incanter.core :as ic])
  (:import (java.util Date UUID)
           (java.text SimpleDateFormat)))


(def schema [{:db/ident       :name
              :db/valueType   :db.type/string
              :db/cardinality :db.cardinality/one}])


(defn create-n-transactions [n]
  (->> (repeatedly #(str (UUID/randomUUID)))
       (take n)
       (mapv (fn [id] {:name id}))))


(defn run-combinations [iterations]
  "Returns observation in following order:
   [:backend :schema-on-read :temporal-index :datoms :mean :sd]"
  (let [header [:backend :schema-on-read :temporal-index :datoms :mean :sd]
        res (for [d-count [1]                               ;;[1 2 4 8 16 32 64 128 256 512 1024]
                  uri uri/all
                  :let [_ (println "Datoms " d-count " " uri)
                        tx (create-n-transactions d-count)
                        sor (:schema-on-read uri)
                        ti (:temporal-index uri)]]
              (do (db/prepare-db (:lib uri) (:uri uri) (if sor [] schema) tx :schema-on-read sor :temporal-index ti)
                  (let [t (measure-connect-times iterations (:lib uri) (:uri uri))]
                    [(:name uri) sor ti d-count (:mean t) (:sd t)])))]
    [header res]))


(defn get-connect-times [file-suffix]
  (let [[header res] (run-combinations 256)
        data (ic/dataset header res)]
    (ic/save data (str c/data-dir "/" (.format c/date-formatter (Date.)) "-" file-suffix ".dat"))))


;;(get-connect-times "conn-times")