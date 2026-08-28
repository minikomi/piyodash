(ns piyodash.solid-foods-test
  (:require [clojure.test :refer [deftest is]]
            [piyodash.solid-foods :as foods]))

(deftest recognizes-ingredients-across-separators-and-recipes
  (let [events [{:event_time 1000 :memo "お粥、初めての豆腐"}
                {:event_time 2000 :memo "バナナパンがゆ ささみポタージュ"}
                {:event_time 3000 :memo "たいと野菜の出汁煮 バナナきなこヨーグルト"}]
        result (foods/analyze events)
        tried (into {} (map (juxt :id identity) (:tried result)))]
    (is (= 3 (:total-events result)))
    (is (= 1000 (:event_time (:first-event (:rice tried)))))
    (is (= 2 (:times (:banana tried))))
    (is (every? tried [:tofu :wheat :chicken :white-fish :kinako :yogurt]))))

(deftest does-not-confuse-white-fish-with-egg-white
  (let [result (foods/analyze [{:event_time 1000 :memo "白身魚のお粥"}])
        allergens (into {} (map (juxt :id :tried?) (:allergens result)))]
    (is (true? (:fish allergens)))
    (is (false? (:egg allergens)))))

(deftest does-not-read-nori-inside-apple-phrase
  (let [result (foods/analyze [{:event_time 1000 :memo "お粥 初めてのりんご"}])
        tried-ids (set (map :id (:tried result)))]
    (is (contains? tried-ids :apple))
    (is (not (contains? tried-ids :nori)))))
