(ns piyodash.sync-test
  (:require [clojure.test :refer [deftest is]]
            [next.jdbc :as jdbc]
            [piyodash.sync :as sync])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-database []
  (str (Files/createTempFile "piyodash-sync-" ".sqlite3"
                             (make-array FileAttribute 0))))

(deftest stores-and-updates-api-records
  (let [path (temp-database)
        response {:status 200
                  :main_version 1
                  :minor_version 42
                  :data {:baby_event [{:baby_id "baby" :event_id "milk-1"
                                       :type 2 :amount 120 :datetime 1000}]}}]
    (is (= 1 (sync/process-response! path "force_sync_to_app" response)))
    (sync/process-response!
     path "sync"
     (assoc-in response [:data :baby_event 0 :amount] 150))
    (let [ds (jdbc/get-datasource {:dbtype "sqlite" :dbname path})
          row (jdbc/execute-one!
               ds
               ["SELECT COUNT(*) AS total,
                        json_extract(payload_json, '$.amount') AS amount
                 FROM records"])]
      (is (= 1 (:total row)))
      (is (= 150 (:amount row))))))

(deftest incremental-sync-sends-stored-version
  (let [path (temp-database)
        credentials (str (Files/createTempFile "piyodash-credentials-" ".json"
                                               (make-array FileAttribute 0)))
        request (atom nil)]
    (spit credentials
          "{\"user_id\":\"user\",\"client_id\":1,\"client_token\":\"token\"}")
    (sync/process-response! path "force_sync_to_app"
                            {:status 200 :main_version 3 :minor_version 91 :data {}})
    (with-redefs-fn
      {(ns-resolve 'piyodash.sync 'post!)
       (fn [endpoint payload]
         (reset! request [endpoint payload])
         {:status 200 :main_version 3 :minor_version 91 :data {}})}
      #(sync/sync! {:credentials credentials :database path}))
    (is (= "sync" (first @request)))
    (is (= 3 (:main_version (second @request))))
    (is (= 91 (:minor_version (second @request))))
    (is (= "PiyoDash" (:app (second @request))))))

(deftest stable-record-keys
  (is (= "baby:event" (sync/record-key :baby_event
                                        {:baby_id "baby" :event_id "event"})))
  (is (= "singleton" (sync/record-key :purchase_info {:anything true}))))
