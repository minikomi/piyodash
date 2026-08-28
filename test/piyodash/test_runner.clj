(ns piyodash.test-runner
  (:require [clojure.test :as test]
            [piyodash.db-test]
            [piyodash.solid-foods-test]
            [piyodash.sync-test]
            [piyodash.view-test]))

(defn -main [& _]
  (let [{:keys [fail error]}
        (test/run-tests 'piyodash.db-test 'piyodash.solid-foods-test
                        'piyodash.sync-test 'piyodash.view-test)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
