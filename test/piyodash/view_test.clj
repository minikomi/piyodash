(ns piyodash.view-test
  (:require [clojure.test :refer [deftest is]]
            [piyodash.view :as view])
  (:import [java.time Instant ZoneId]))

(deftest formats-duration
  (let [now (Instant/ofEpochSecond 100000)]
    (is (= "0m 42s" (view/duration-label (.minusSeconds now 42) now)))
    (is (= "2h 03m" (view/duration-label (.minusSeconds now 7380) now)))
    (is (= "1d 2h" (view/duration-label (.minusSeconds now 93600) now)))
    (is (= "—" (view/duration-label nil now)))))

(deftest labels-most-recent-state
  (let [models (view/panel-models
                {:sleep {:event_type 4}
                 :diaper {:event_type 7}}
                (Instant/now)
                (ZoneId/of "Asia/Tokyo"))
        diaper (some #(when (= "diaper-panel" (:id %)) %) models)]
    (is (= "睡眠" (:label-ja (first models))))
    (is (= "Sleep" (:label-en (first models))))
    (is (= "うんち" (:label-ja diaper)))
    (is (= "Poop" (:label-en diaper)))))

(deftest labels-japanese-state
  (let [models (view/panel-models
                {:sleep {:event_type 5}
                 :diaper {:event_type 6}}
                (Instant/now)
                (ZoneId/of "Asia/Tokyo"))
        diaper (some #(when (= "diaper-panel" (:id %)) %) models)]
    (is (= "起きている" (:label-ja (first models))))
    (is (= "Awake" (:label-en (first models))))
    (is (= "おしっこ" (:label-ja diaper)))
    (is (= "Pee" (:label-en diaper)))))
