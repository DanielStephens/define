(ns define.modelling.transformation
  (:require [clojure.core.matrix :as m]
            [clj-fuzzy.metrics :as met]))

(def m -1)
(def M 1)
(def r (- M m))

(defn- normalise [min max value]
  (let [range (- max min)
        n-v (/ (- value min) range)
        o-v (+ (* n-v r) m)]
    o-v))

(defn- de-normalise [min max value]
  (let [range (- max min)
        n-v (/ (- value m) r)
        o-v (+ (* n-v range) min)]
    o-v))

(defn- abs [x] (if (neg? x)
                 (* -1 x)
                 x))

(defn- zip [coll1 coll2]
  (map vector coll1 coll2))

(defn- field-spec [record]
  (map
    #(select-keys
       %
       [:name
        :type
        :options])
    record))

(defn- flatten-record [record]
  (map :value record))

(defn- importance-collector [important coll]
  (let [c (count coll)
        i (* important c)
        f (frequencies coll)]
    (->> f
         (sort-by second)
         (reverse)
         (filter #(>= (second %) i))
         (map first))))

(defn- noise? [coll]
  (let [t (inc (count (distinct coll)))
        i (* (/ 1 t) 1.2)
        f (importance-collector i coll)]
    (and
      (> t 50)
      (empty? f))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FIELD TRANSFORMERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmulti get-transformer (fn [{:keys [type]} _]
                            type))

(defmethod get-transformer :default [_ _]
  {:discreter (fn [_] 0)
   :populator (fn [_] :unknown)
   :measure (fn [_ _] 0)})

(defmethod get-transformer :integer [_ col]
  (let [min (apply min col)
        max (apply max col)]
    {:discreter (fn [x] (normalise min max x))
     :populator (fn [x] (Math/round (double (de-normalise min max x))))
     :measure   (fn [x y] (/ (abs (- x y)) (- max min)))}))

(defmethod get-transformer :enum [{:keys [options]} _]
  (let [min 0
        max (count options)]
    {:discreter (fn [x] (->> x
                             (.indexOf options)
                             (inc)
                             (normalise min max)))
     :populator (fn [x]
                  (let [v (de-normalise min max x)
                        index (Math/round (double (dec v)))]
                    (nth options index :unknown)))
     :measure (fn [x y] (if (= x y)
                          0
                          1))}))

(defmethod get-transformer :string [_ col]
  (if (noise? col)
    (get-transformer {:type :default} col)
    (let [is (importance-collector 0.05 col)
          ]
      (merge
        (get-transformer {:type :enum :options is} col)
        {:measure (fn [x y] (met/levenshtein x y))}))))

(defn- typed-transformer [[f col]]
  (get-transformer f col))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; RECORD TRANSFORMERS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Transformer
  (discrete [t record])
  (populate [t vec])
  (measure [t r-1 r-2]))

(defrecord SimpleTransformer [spec ds ps ms]
  Transformer
  (discrete [_ record]
    (->> record
         (map :value)
         (zip ds)
         (map (fn [x]
                ((first x) (second x))))))
  (populate [_ vec]
    (->> vec
         (zip ps)
         (map (fn [x]
                ((first x) (second x))))
         (map (fn [x] {:value x}))
         (map (fn [x y] (merge x y)) spec)))
  (measure [_ r-1 r-2]
    (let [val-fn (fn [record] (map :value record))
          vr-1 (val-fn r-1)
          vr-2 (val-fn r-2)
          d (map (fn [m f-1 f-2] (m f-1 f-2)) ms vr-1 vr-2)
          s-d (map (fn [x] (* x x)) d)]
      (Math/sqrt (reduce + s-d)))))

(defn transformer [records]
  (assert (> (count records) 1) "at least one group is required to start measuring significance of values")
  (let [types (field-spec (first records))
        data-rows (map flatten-record records)
        data-cols (m/columns data-rows)
        t-cols (zip types data-cols)
        ts (map typed-transformer t-cols)
        ds (map :discreter ts)
        ps (map :populator ts)
        ms (map :measure ts)]
    (SimpleTransformer. types ds ps ms)))

(defn- map-map [k-f v-f m]
  (into {} (for [[k v] m] [(k-f k) (v-f v)])))

(defn group->input [transformer group]
  (map-map identity #(discrete transformer %) group))

(defn record->input [transformer record]
  {:input (discrete transformer record)})

(defn output->record [transformer output]
  (populate transformer (:output output)))

(defn groups->records [groups]
  (concat (map :input groups) (map :output groups)))