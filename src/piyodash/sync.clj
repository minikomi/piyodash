(ns piyodash.sync
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs])
  (:import [java.net URI]
           [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers
            HttpResponse$BodyHandlers]
           [java.security MessageDigest]
           [java.time Instant]))

(def api-root "https://api2.piyolog.com")

(def ^:private record-key-fields
  {:baby [:baby_id]
   :baby_event [:baby_id :event_id]
   :day_log [:baby_id :date]
   :food_record [:baby_id :food_id]
   :calendar_event [:baby_id :event_id]})

(def ^:private singleton-entities #{:custom_event_info :purchase_info})

(defn- read-json [text]
  (json/read-str text :key-fn keyword))

(defn load-credentials [path]
  (let [credentials (read-json (slurp path))]
    (when-not (every? credentials [:user_id :client_id :client_token])
      (throw (ex-info (str "Credential file is incomplete: " path) {:path path})))
    credentials))

(defn- post! [endpoint payload]
  (let [body (json/write-str payload)
        request (-> (HttpRequest/newBuilder (URI/create (str api-root "/" endpoint)))
                    (.header "Content-Type" "application/json; charset=utf-8")
                    (.header "User-Agent" "PiyoDash/0.2")
                    (.POST (HttpRequest$BodyPublishers/ofString body))
                    (.build))
        response (.send (HttpClient/newHttpClient)
                        request
                        (HttpResponse$BodyHandlers/ofString))]
    (when-not (= 200 (.statusCode response))
      (throw (ex-info (str "HTTP " (.statusCode response) " from " endpoint)
                      {:endpoint endpoint :status (.statusCode response)})))
    (let [result (read-json (.body response))]
      (when-not (= 200 (:status result))
        (throw (ex-info (str "PiyoLog " endpoint " failed (status "
                             (:status result) ": "
                             (or (:message result) "unknown error") ")")
                        {:endpoint endpoint :response result})))
      result)))

(defn- sha256 [text]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes text "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn record-key [entity item]
  (cond
    (singleton-entities entity) "singleton"
    (and (record-key-fields entity)
         (every? #(contains? item %) (record-key-fields entity)))
    (str/join ":" (map item (record-key-fields entity)))
    :else (sha256 (json/write-str (into (sorted-map) item)))))

(defn- event-time [item]
  (some (fn [field]
          (let [value (get item field)]
            (cond
              (number? value) (long value)
              (and (string? value) (re-matches #"\d+" value)) (parse-long value))))
        [:datetime2 :datetime :start :date :created_at]))

(defn- records [data]
  (mapcat (fn [[entity value]]
            (cond
              (sequential? value) (map #(vector entity %) (filter map? value))
              (map? value) [[entity value]]
              :else []))
          data))

(defn ensure-schema! [database]
  (when-let [parent (.getParentFile (io/file database))]
    (.mkdirs parent))
  (let [ds (jdbc/get-datasource {:dbtype "sqlite" :dbname database})]
    (jdbc/execute! ds ["PRAGMA journal_mode=WAL"])
    (jdbc/execute! ds ["CREATE TABLE IF NOT EXISTS sync_runs (
                          id INTEGER PRIMARY KEY,
                          endpoint TEXT NOT NULL,
                          fetched_at TEXT NOT NULL,
                          response_json TEXT NOT NULL,
                          processed INTEGER NOT NULL DEFAULT 0)"])
    (jdbc/execute! ds ["CREATE TABLE IF NOT EXISTS records (
                          entity TEXT NOT NULL,
                          record_key TEXT NOT NULL,
                          baby_id TEXT,
                          event_time INTEGER,
                          modified_at INTEGER,
                          deleted INTEGER NOT NULL DEFAULT 0,
                          payload_json TEXT NOT NULL,
                          seen_at TEXT NOT NULL,
                          PRIMARY KEY (entity, record_key))"])
    (jdbc/execute! ds ["CREATE INDEX IF NOT EXISTS records_by_baby_time
                        ON records (baby_id, event_time)"])
    ds))

(defn process-response! [database endpoint response]
  (let [ds (ensure-schema! database)
        fetched-at (str (Instant/now))
        items (records (or (:data response) {}))]
    (jdbc/with-transaction [tx ds]
      (jdbc/execute! tx
                     ["INSERT INTO sync_runs(endpoint, fetched_at, response_json, processed)
                       VALUES (?, ?, ?, 1)"
                      endpoint fetched-at (json/write-str response)])
      (doseq [[entity item] items]
        (jdbc/execute!
         tx
         ["INSERT INTO records (
              entity, record_key, baby_id, event_time, modified_at,
              deleted, payload_json, seen_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(entity, record_key) DO UPDATE SET
              baby_id = excluded.baby_id,
              event_time = excluded.event_time,
              modified_at = excluded.modified_at,
              deleted = excluded.deleted,
              payload_json = excluded.payload_json,
              seen_at = excluded.seen_at"
          (name entity) (record-key entity item) (:baby_id item)
          (event-time item) (:modified_at item) (if (:deleted item) 1 0)
          (json/write-str item) fetched-at])))
    (count items)))

(defn- last-version [ds]
  (jdbc/execute-one!
   ds
   ["SELECT
       CAST(json_extract(response_json, '$.main_version') AS INTEGER) AS main_version,
       CAST(json_extract(response_json, '$.minor_version') AS INTEGER) AS minor_version
     FROM sync_runs
     WHERE json_extract(response_json, '$.main_version') IS NOT NULL
       AND json_extract(response_json, '$.minor_version') IS NOT NULL
     ORDER BY id DESC LIMIT 1"]
   {:builder-fn rs/as-unqualified-lower-maps}))

(defn sync! [{:keys [credentials database]}]
  (let [ds (ensure-schema! database)
        version (last-version ds)
        endpoint (if version "sync" "force_sync_to_app")
        auth (select-keys (load-credentials credentials)
                          [:user_id :client_id :client_token])
        payload (cond-> (assoc auth :api_version 2.0)
                  version (assoc :main_version (:main_version version)
                                 :minor_version (:minor_version version)
                                 :app "PiyoDash"))
        response (post! endpoint payload)]
    {:endpoint endpoint
     :records (process-response! database endpoint response)}))

(defn enroll! [{:keys [credentials database]} user-id share-code]
  (let [result (post! "share_code_confirm"
                      {:api_version 2.0
                       :user_id user-id
                       :code share-code
                       :name "PiyoDash remote collector"
                       :type 1})
        saved (select-keys result [:user_id :client_id :client_token :role])]
    (when-let [parent (.getParentFile (io/file credentials))]
      (.mkdirs parent))
    (spit credentials (str (json/write-str saved :indent true) "\n"))
    (.setReadable (io/file credentials) false false)
    (.setWritable (io/file credentials) false false)
    (.setReadable (io/file credentials) true true)
    (.setWritable (io/file credentials) true true)
    (sync! {:credentials credentials :database database})))

(defn -main [& [command]]
  (when-not (= command "enroll")
    (throw (ex-info "Usage: clojure -M:enroll" {})))
  (print "PiyoLog ID: ")
  (flush)
  (let [user-id (str/trim (read-line))
        console (System/console)
        share-code (if console
                     (String. (.readPassword console "One-time Share Code: " (object-array 0)))
                     (do (print "One-time Share Code: ") (flush) (read-line)))
        result (enroll! {:credentials ".piyolog.json" :database "piyolog.sqlite3"}
                        user-id (str/trim share-code))]
    (println (str "Enrolled and synced " (:records result) " records"))))
