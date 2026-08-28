(ns piyodash.db-test
  (:require [clojure.test :refer [deftest is testing]]
            [next.jdbc :as jdbc]
            [piyodash.db :as db])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-database []
  (str (Files/createTempFile "piyodash-" ".sqlite3"
                             (make-array FileAttribute 0))))

(deftest finds-latest-events
  (let [path (temp-database)
        ds (db/datasource path)]
    (jdbc/execute! ds ["CREATE TABLE records (
                          entity TEXT, baby_id TEXT, event_time INTEGER,
                          modified_at INTEGER, deleted INTEGER, payload_json TEXT)"])
    (doseq [[time type deleted]
            [[1000 2 0] [2000 1 0] [3000 4 0] [4000 5 0]
             [5000 6 0] [6000 7 0] [7000 2 1]]]
      (jdbc/execute! ds
                     ["INSERT INTO records VALUES ('baby_event', 'one', ?, ?, ?, ?)"
                      time time deleted (str "{\"type\":" type
                                             (when (= type 2) ",\"amount\":120") "}")]))
    (let [events (db/dashboard-events ds "one")]
      (is (= 4000 (:event_time (:sleep events))))
      (is (= 1000 (:event_time (:milk events))))
      (is (= 120.0 (:amount (:milk events))))
      (is (= 2000 (:event_time (:breastfeed events))))
      (is (= 6000 (:event_time (:diaper events)))))))
