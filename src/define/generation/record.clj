(ns define.generation.record
  (:require [clojure.spec.alpha :as s]))

(defn- map-map [k-f v-f map]
  (into {} (for [[k v] map] [(k-f k) (v-f v)])))

(defn- map-keys [k-f map]
  (map-map k-f identity map))

(defn- namespace-map [map]
  (map-keys #(keyword "define.generation.record" (name %)) map))

(defn- de-namespace-map [map]
  (map-keys #(keyword (name %)) map))

(defmulti value-spec (fn [field] (::type field)))

(s/def ::value-conf (s/conformer (fn [x] (::value x))))

(s/def ::options coll?)

(defmethod value-spec :integer [field]
  (s/and
    ::value-conf
    integer?))

(defmethod value-spec :string [field]
  (s/and
    ::value-conf
    string?))

(defmethod value-spec :enum [field]
  (s/and
    (s/keys :req [::options])
    (fn [x] (.contains (::options x) (::value x)))))

(defn value-valid-for-type [x]
  (s/valid? (value-spec x) x))

(s/def ::name keyword?)

(s/def ::type #{:integer
                :string
                :enum})

(s/def ::value any?)

(s/def ::field (s/and
                 map?
                 (s/conformer namespace-map)
                 (s/keys :req [::name ::type ::value])
                 value-valid-for-type
                 (s/conformer de-namespace-map)))

(s/def ::record
  (s/coll-of ::field))