(ns piyodash.db
  (:require [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import [java.nio.file Files LinkOption Paths]
           [java.time Instant]))

(def event-types
  {:breastfeed 1
   :milk 2
   :sleep 4
   :wake 5
   :pee 6
   :poop 7})

(defn datasource [path]
  (jdbc/get-datasource {:dbtype "sqlite" :dbname path}))

(defn database-exists? [path]
  (Files/exists (Paths/get path (make-array String 0))
                (make-array LinkOption 0)))

(defn- baby-clause [baby-id]
  (if (seq baby-id)
    [" AND baby_id = ?" [baby-id]]
    ["" []]))

(defn latest-event
  "Returns the newest non-deleted baby event matching any supplied type."
  [ds types baby-id]
  (let [[baby-sql baby-params] (baby-clause baby-id)
        placeholders (str/join "," (repeat (count types) "?"))
        sql (str "SELECT baby_id, event_time, modified_at, "
                 "CAST(json_extract(payload_json, '$.type') AS INTEGER) AS event_type, "
                 "CAST(json_extract(payload_json, '$.amount') AS REAL) AS amount "
                 "FROM records "
                 "WHERE entity = 'baby_event' AND deleted = 0 "
                 "AND CAST(json_extract(payload_json, '$.type') AS INTEGER) IN ("
                 placeholders ")"
                 baby-sql
                 " ORDER BY COALESCE(event_time, modified_at) DESC LIMIT 1")]
    (first
     (jdbc/execute! ds
                    (into [sql] (concat types baby-params))
                    {:builder-fn rs/as-unqualified-lower-maps}))))

(defn event-instant [{:keys [event_time modified_at]}]
  (when-let [raw (or event_time modified_at)]
    ;; PiyoLog's datetime2/modified_at values are milliseconds. Accept seconds
    ;; as well so imported or hand-built databases remain useful.
    (Instant/ofEpochMilli
     (if (< (long raw) 100000000000)
       (* 1000 (long raw))
       (long raw)))))

(defn dashboard-events [ds baby-id]
  {:sleep (latest-event ds [(event-types :sleep) (event-types :wake)] baby-id)
   :milk (latest-event ds [(event-types :milk)] baby-id)
   :breastfeed (latest-event ds [(event-types :breastfeed)] baby-id)
   :diaper (latest-event ds [(event-types :pee) (event-types :poop)] baby-id)})

(defn solid-food-events
  "Returns non-deleted solid-food events, oldest first. PiyoLog stores solid
  food as baby event type 9 and records the meal as free text in memo."
  [ds baby-id]
  (let [[baby-sql baby-params] (baby-clause baby-id)
        sql (str "SELECT baby_id, event_time, modified_at, "
                 "COALESCE(json_extract(payload_json, '$.memo'), '') AS memo "
                 "FROM records "
                 "WHERE entity = 'baby_event' AND deleted = 0 "
                 "AND CAST(json_extract(payload_json, '$.type') AS INTEGER) = 9"
                 baby-sql
                 " ORDER BY COALESCE(event_time, modified_at) ASC")]
    (jdbc/execute! ds
                   (into [sql] baby-params)
                   {:builder-fn rs/as-unqualified-lower-maps})))
