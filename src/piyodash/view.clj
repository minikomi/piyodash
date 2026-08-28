(ns piyodash.view
  (:require [hiccup2.core :as h]
            [piyodash.db :as db])
  (:import [java.time Duration Instant ZoneId]
           [java.time.format DateTimeFormatter]))

(def datastar-url
  "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC.6/bundles/datastar.js")

(def ^:private copy
  {:en {:current-sleep "Current sleep"
        :current-wake "Current wake window"
        :asleep-for "Asleep for"
        :awake-for "Awake for"
        :sleep-wake "Sleep / wake"
        :fell-asleep "Fell asleep at"
        :woke-up "Woke up at"
        :bottle "Bottle"
        :since-milk "Since last milk"
        :last-milk "Last milk"
        :fed-at "Fed at"
        :breastfeeding "Breastfeeding"
        :since-breastfeed "Since last breastfeed"
        :last-breastfeed "Last breastfeed"
        :diaper "Diaper"
        :since-pee "Since last pee"
        :since-poop "Since last poop"
        :since-diaper "Since last diaper"
        :last-diaper "Last diaper"
        :pee-at "Pee at"
        :poop-at "Poop at"
        :changed-at "Changed at"
        :waiting-data "Waiting for PiyoLog data"
        :waiting-db "Waiting for database"}
   :ja {:current-sleep "睡眠中"
        :current-wake "起きている時間"
        :asleep-for "寝てから"
        :awake-for "起きてから"
        :sleep-wake "睡眠・起床"
        :fell-asleep "寝た時刻"
        :woke-up "起きた時刻"
        :bottle "ミルク"
        :since-milk "最後のミルクから"
        :last-milk "最後のミルク"
        :fed-at "飲んだ時刻"
        :breastfeeding "母乳"
        :since-breastfeed "最後の授乳から"
        :last-breastfeed "最後の授乳"
        :diaper "おむつ"
        :since-pee "最後のおしっこから"
        :since-poop "最後のうんちから"
        :since-diaper "最後のおむつから"
        :last-diaper "最後のおむつ"
        :pee-at "おしっこの時刻"
        :poop-at "うんちの時刻"
        :changed-at "替えた時刻"
        :waiting-data "ぴよログのデータを待っています"
        :waiting-db "データベースを待っています"}})

(defn- t [language key]
  (get-in copy [language key] (name key)))

(defn- elapsed-seconds [^Instant then ^Instant now]
  (when then
    (max 0 (.getSeconds (Duration/between then now)))))

(defn duration-label [^Instant then ^Instant now]
  (if-not then
    "—"
    (let [seconds (elapsed-seconds then now)
          days (quot seconds 86400)
          hours (quot (mod seconds 86400) 3600)
          minutes (quot (mod seconds 3600) 60)
          secs (mod seconds 60)]
      (cond
        (pos? days) (format "%dd %dh" days hours)
        (pos? hours) (format "%dh %02dm" hours minutes)
        :else (format "%dm %02ds" minutes secs)))))

(defn- clock-label [^Instant instant ^ZoneId zone language]
  (when instant
    (.format (DateTimeFormatter/ofPattern (if (= language :ja) "H:mm" "h:mm a"))
             (.atZone instant zone))))

(defn- panel-icon [icon]
  (let [common {:viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
                :stroke-width "1.8" :stroke-linecap "round"
                :stroke-linejoin "round" :aria-hidden "true"}
        phosphor {:viewBox "0 0 256 256" :fill "currentColor"
                  :aria-hidden "true"}]
    (case icon
      :baby-awake
      [:svg.panel-icon phosphor
       [:path {:d "M92,140a12,12,0,1,1,12-12A12,12,0,0,1,92,140Zm72-24a12,12,0,1,0,12,12A12,12,0,0,0,164,116Zm-12.27,45.23a45,45,0,0,1-47.46,0,8,8,0,0,0-8.54,13.54,61,61,0,0,0,64.54,0,8,8,0,0,0-8.54-13.54ZM232,128A104,104,0,1,1,128,24,104.11,104.11,0,0,1,232,128Zm-16,0a88.11,88.11,0,0,0-84.09-87.91C120.32,56.38,120,71.88,120,72a8,8,0,0,0,16,0,8,8,0,0,1,16,0,24,24,0,0,1-48,0c0-.73.13-14.3,8.46-30.63A88,88,0,1,0,216,128Z"}]]
      :baby-sleep
      [:svg.panel-icon.sleep-baby
       {:viewBox "0 0 256 256" :fill "none" :stroke "currentColor"
        :stroke-width "14" :stroke-linecap "round"
        :stroke-linejoin "round" :aria-hidden "true"}
       [:circle {:cx "128" :cy "132" :r "92"}]
       [:path {:d "M111 43c-7 15-5 28 8 34 15 7 29-4 29-18"}]
       [:path {:d "M72 132c12 12 28 12 40 0M144 132c12 12 28 12 40 0"}]
       [:path {:d "M104 170c15 9 33 9 48 0"}]
       [:path {:d "M176 70h27l-27 27h27M207 45h18l-18 18h18" :stroke-width "10"}]]
      :bottle [:svg.panel-icon common
               [:path {:d "M9 3h6M10 3v3l-2 2v11a2 2 0 0 0 2 2h4a2 2 0 0 0 2-2V8l-2-2V3M8 11h8M9.5 15h5"}]]
      :breastfeed [:svg.panel-icon common
                   [:path {:d "M12 20s-7-4.35-7-10a4 4 0 0 1 7-2.65A4 4 0 0 1 19 10c0 5.65-7 10-7 10Z"}]
                   [:path {:d "M12 10.3c1.35 1.8 2.1 2.8 2.1 4a2.1 2.1 0 0 1-4.2 0c0-1.2.75-2.2 2.1-4Z"}]]
      :pee [:svg.panel-icon common
            [:path {:d "M12 2.5S6.5 9 6.5 14a5.5 5.5 0 0 0 11 0C17.5 9 12 2.5 12 2.5Z"}]
            [:path {:d "M9.3 15.2c.4 1.3 1.3 2 2.7 2.2"}]]
      :poop [:svg.panel-icon common
             [:path {:d "M8.2 10.5c-1.8 0-3.2 1.2-3.2 2.8 0 1 .5 1.8 1.3 2.3-1 .4-1.6 1.3-1.6 2.4 0 1.7 1.5 3 3.4 3h7.8c1.9 0 3.4-1.3 3.4-3 0-1.2-.7-2.2-1.9-2.6.8-.5 1.3-1.4 1.3-2.4 0-1.7-1.6-3-3.6-3h-.2c.3-.5.5-1 .5-1.6 0-1.7-1.5-3-3.4-3 .1-1.3-.4-2.3-1.5-3.1.2 2.6-1 3.5-2.8 4.2"}]])))

(defn panel-models [events _now _zone]
  (let [sleep-event (:sleep events)
        sleep-type (:event_type sleep-event)
        sleeping? (= (db/event-types :sleep) sleep-type)
        diaper-event (:diaper events)
        diaper-type (:event_type diaper-event)]
    [{:id "sleep-panel"
      :class "sleep"
      :icon (if sleeping? :baby-sleep :baby-awake)
      :label-ja (if sleeping? "睡眠" "起きている")
      :label-en (if sleeping? "Sleep" "Awake")
      :event sleep-event}
     {:id "diaper-panel"
      :class (str "diaper " (if (= diaper-type (db/event-types :poop)) "poop" "pee"))
      :icon (if (= diaper-type (db/event-types :poop)) :poop :pee)
      :label-ja (case diaper-type 6 "おしっこ" 7 "うんち" "おむつ")
      :label-en (case diaper-type 6 "Pee" 7 "Poop" "Diaper")
      :event diaper-event}
     {:id "milk-panel"
      :class "milk"
      :icon :bottle
      :label-ja "ミルク"
      :label-en "Milk"
      :event (:milk events)
      :amount (:amount (:milk events))}
     {:id "breastfeed-panel"
      :class "breastfeed"
      :icon :breastfeed
      :label-ja "母乳"
      :label-en "Breastfeed"
      :event (:breastfeed events)}]))

(defn- amount-label [amount]
  (when (number? amount)
    (str (if (== (double amount) (Math/floor (double amount)))
           (format "%.0f" (double amount))
           (format "%.1f" (double amount)))
         " ml")))

(def ^:private yellow-threshold-seconds
  {"sleep" (* 150 60)
   "milk" (* 3 60 60)
   "breastfeed" (* 3 60 60)})

(defn- best-time [panel-class ^Instant instant]
  (when-let [seconds (get yellow-threshold-seconds panel-class)]
    (when instant
      (.plusSeconds instant seconds))))

(defn- elapsed-style [panel-class ^Instant instant ^Instant now]
  (when instant
    (let [seconds (elapsed-seconds instant now)]
      (cond
        ;; Start with light yellow at 2h 30m and reach red one hour later.
        (= panel-class "sleep")
        (when (>= seconds (* 150 60))
          (let [progress (min 1.0 (/ (- seconds (* 150 60)) 3600.0))]
            {:class "elapsed-sleep-alert"
             :style (format "--elapsed-alert: %.1f%%" (* 100 progress))}))

        (or (= panel-class "milk") (= panel-class "breastfeed"))
        (cond
          (>= seconds (* 4 60 60)) {:class "elapsed-danger"}
          (>= seconds (* 3 60 60)) {:class "elapsed-warning"})))))

(defn panel [{:keys [id class icon label-ja label-en event amount]}
             now zone]
  (let [instant (db/event-instant event)
        alert (elapsed-style class instant now)
        best-time-instant (best-time class instant)]
    [:section.panel {:id id :class class}
     (panel-icon icon)
     [:div.panel-copy
      [:h2 {:lang "ja"} label-ja]
      [:p.translation {:lang "en"} label-en]]
     [:div.elapsed-block
      [:div.elapsed (cond-> {}
                      (:class alert) (assoc :class (:class alert))
                      (:style alert) (assoc :style (:style alert)))
       (duration-label instant now)]
      (when best-time-instant
        [:time.best-time (clock-label best-time-instant zone :en)])
      (when-let [formatted (amount-label amount)]
        [:div.milk-amount formatted])]]))

(defn dashboard [events now zone]
  [:main#dashboard
   {"data-on-interval__duration.1s" "@get('/dashboard')"
    :aria-live "polite"}
   (map #(panel % now zone) (panel-models events now zone))])

(defn unavailable-dashboard [message]
  [:main#dashboard
   {"data-on-interval__duration.5s" "@get('/dashboard')"}
   [:section.panel.unavailable
    [:p.eyebrow "PiyoDash"]
    [:h2 {:lang "ja"} (t :ja :waiting-db)]
    [:p.translation {:lang "en"} (t :en :waiting-db)]
    [:p.detail message]]])

(defn page [dashboard-html]
  (str
   "<!doctype html>"
   (h/html
    [:html {:lang "ja"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1, viewport-fit=cover"}]
      [:meta#theme-color {:name "theme-color" :content "#f2f0e9"}]
      [:meta {:name "apple-mobile-web-app-capable" :content "yes"}]
      [:meta {:name "apple-mobile-web-app-status-bar-style"
              :content "black-translucent"}]
      [:meta {:name "apple-mobile-web-app-title" :content "PiyoDash"}]
      [:title "PiyoDash"]
      [:link {:rel "manifest" :href "/manifest.webmanifest"}]
      [:script
       (h/raw
        "(()=>{const saved=localStorage.getItem('piyodash-theme');const theme=saved||(matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light');document.documentElement.dataset.theme=theme;window.setPiyoTheme=(next)=>{document.documentElement.dataset.theme=next;localStorage.setItem('piyodash-theme',next);document.querySelector('#theme-color')?.setAttribute('content',next==='dark'?'#161714':'#f2f0e9');document.querySelector('#theme-toggle')?.setAttribute('aria-label',next==='dark'?'Use light mode':'Use dark mode')};window.togglePiyoTheme=()=>setPiyoTheme(document.documentElement.dataset.theme==='dark'?'light':'dark')})()")]
      [:link {:rel "stylesheet" :href "/app.css?v=9"}]
      [:script {:type "module" :src datastar-url}]]
     [:body
      [:button#theme-toggle
       {:type "button" :aria-label "Switch color mode"
        :onclick "togglePiyoTheme()"}
       [:svg.sun-icon {:viewBox "0 0 24 24" :aria-hidden "true"}
        [:circle {:cx "12" :cy "12" :r "3.5"}]
        [:path {:d "M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.65 17.65l1.42 1.42M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.65 6.35l1.42-1.42"}]]
       [:svg.moon-icon {:viewBox "0 0 24 24" :aria-hidden "true"}
        [:path {:d "M20.2 15.2A8.5 8.5 0 0 1 8.8 3.8a8.5 8.5 0 1 0 11.4 11.4Z"}]]]
      dashboard-html
      [:script (h/raw "setPiyoTheme(document.documentElement.dataset.theme)")]]])))

(defn render [value]
  (str (h/html value)))
