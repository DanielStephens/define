(ns define.generation.group
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [define.generation.record]
            [clojure.test.check.generators])
  (:import (java.util UUID)))

(defn record-gen [t v d]
  [
   {:name :identifier
    :type :string
    :value (.toString (UUID/randomUUID))}
   {:name :type
    :type :enum
    :options [:credit :debit]
    :value t}
   {:name :value
    :type :integer
    :value v}
   {:name :day
    :type :integer
    :value (if (= :credit t)
             d
             (inc d))}
   ])

(defn group-gen []
  (let [r (rand-int 2)
        types [:credit :debit]
        f-t (nth types r)
        s-t (nth types (- 1 r))
        v (rand-int 1000)
        d (rand-int 99)]
    (clojure.test.check.generators/return {:input  (record-gen f-t v d)
                                           :output (record-gen s-t v d)})))

(defn- map-map [k-f v-f map]
  (into {} (for [[k v] map] [(k-f k) (v-f v)])))

(defn- map-keys [k-f map]
  (map-map k-f identity map))

(defn- namespace-map [map]
  (map-keys #(keyword "define.generation.group" (name %)) map))

(defn- de-namespace-map [map]
  (map-keys #(keyword (name %)) map))

(s/def ::input :define.generation.record/record)

(s/def ::output :define.generation.record/record)

(s/def ::group
  (s/with-gen
    (s/and
      (s/conformer namespace-map)
      (s/keys :req [::input ::output])
      (s/conformer de-namespace-map))
    group-gen))

(defn generate-groups [count]
  (take count (repeatedly #(gen/generate (s/gen ::group)))))