(ns piyodash.core
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :as http]
            [piyodash.db :as db]
            [piyodash.sync :as sync]
            [piyodash.view :as view])
  (:import [java.nio.file Paths]
           [java.time Instant ZoneId]))

(defn- env [name]
  (System/getenv name))

(defn config []
  {:database (or (env "PIYOLOG_DB") "piyolog.sqlite3")
   :credentials (or (env "PIYOLOG_CONFIG") ".piyolog.json")
   :sync-interval (or (some-> (env "PIYOLOG_SYNC_INTERVAL") parse-double) 60.0)
   :baby-id (env "BABY_ID")
   :zone (ZoneId/of (or (env "TZ") "Asia/Tokyo"))})

(defn- regular-file? [path]
  (.isFile (.toFile (Paths/get path (make-array String 0)))))

(defn- start-sync-loop!
  [{:keys [credentials sync-interval] :as settings}]
  (if-not (regular-file? credentials)
    (do (println (str "Collector not started: " credentials " was not found; run enroll first")) nil)
    (let [stop (async/chan)]
      (println (str "PiyoLog sync loop is running every " sync-interval " seconds"))
      (async/go-loop []
        (async/<! (async/thread
                    (try
                      (let [{:keys [endpoint records]} (sync/sync! settings)]
                        (println (str "PiyoLog " endpoint ": " records " records")))
                      (catch Exception error
                        (println (str "PiyoLog sync failed: " (ex-message error)))))))
        (let [[_ channel] (async/alts! [stop (async/timeout (long (* 1000 sync-interval)))])]
          (when-not (= channel stop)
            (recur))))
      stop)))

(defn- sse-data [content]
  (->> (str/split-lines content)
       (map #(str "data: elements " %))
       (str/join "\n")))

(defn- patch-response [html]
  {:status 200
   :headers {"content-type" "text/event-stream"
             "cache-control" "no-cache"}
   :body (str "event: datastar-patch-elements\n"
              (sse-data html)
              "\n\n")})

(defn- dashboard-view [{:keys [database baby-id zone]}]
  (if-not (db/database-exists? database)
    (view/unavailable-dashboard (str "No database found at " database))
    (try
      (view/dashboard
       (db/dashboard-events (db/datasource database) baby-id)
       (Instant/now)
       zone)
      (catch Exception error
        (view/unavailable-dashboard (or (ex-message error) "Could not read SQLite"))))))

(defn- solid-foods-view [{:keys [database baby-id zone]}]
  (if-not (db/database-exists? database)
    (view/unavailable-solid-foods (str "No database found at " database))
    (try
      (view/solid-foods-content
       (db/solid-food-events (db/datasource database) baby-id)
       zone)
      (catch Exception error
        (view/unavailable-solid-foods
         (or (ex-message error) "Could not read SQLite"))))))

(defn handler [request]
  (let [settings (config)]
    (case [(:request-method request) (:uri request)]
      [:get "/"] {:status 200
                    :headers {"content-type" "text/html; charset=utf-8"}
                    :body (view/page (dashboard-view settings))}
      [:get "/dashboard"] (patch-response (view/render (dashboard-view settings)))
      [:get "/solidfoods"] {:status 200
                              :headers {"content-type" "text/html; charset=utf-8"}
                              :body (view/solid-foods-page
                                     (solid-foods-view settings))}
      [:get "/solidfoods/content"]
      (patch-response (view/render (solid-foods-view settings)))
      [:get "/app.css"] {:status 200
                           :headers {"content-type" "text/css; charset=utf-8"
                                     "cache-control" "no-cache"}
                           :body (slurp (io/resource "public/app.css"))}
      [:get "/manifest.webmanifest"]
      {:status 200
       :headers {"content-type" "application/manifest+json"}
       :body (slurp (io/resource "public/manifest.webmanifest"))}
      {:status 404 :headers {"content-type" "text/plain"} :body "Not found"})))

(defn -main [& _]
  (let [port (parse-long (or (env "PORT") "3000"))
        settings (config)
        sync-stop (start-sync-loop! settings)
        stop-server (http/run-server #'handler {:host "0.0.0.0" :port port})]
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(do
                 (stop-server)
                 (when sync-stop
                   (async/close! sync-stop)))))
    (println (str "PiyoDash is ready at http://0.0.0.0:" port))
    (println (str "Reading " (:database settings)
                  (when-let [baby-id (:baby-id settings)]
                    (str " for baby " baby-id))))
    @(promise)))
